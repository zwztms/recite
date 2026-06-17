# Phase 7 — report 报告 实施计划

> 日期：2026-06-16 | 状态：计划已审批，待编码

---

## 一、子域定位

report 是支撑域。负责三件事：**报告自动生成**（MQ 异步）、**个人仪表盘**（聚合统计）、**学习档案**（历史查询）。核心亮点是**第一次用上 RocketMQ 做子域异步通信**——finishRecite 发消息后立即返回，消费者异步生成报告，前端轮询取结果。

---

## 二、要实现哪些功能，怎么实现

### 功能 1：MQ 异步生成报告

**做什么**：用户结束背诵 → 发 RocketMQ 消息，立即返回 → 消费者异步调 LLM 生成报告 → 存入 learning_journal → 前端轮询取结果。

**完整调用链路**：

```
POST /recite/{sid}/finish

ReciteController.finishRecite(sid)
  │
  ▼
ReciteOrchestrationService.finishRecite(userId, sid)
  │
  ├─ 查 recite_records（本轮全部记录）
  ├─ Java 基础统计（总分、平均分、模块分布）
  ├─ session.status = FINISHED → 写回 Redis
  │
  ├─ [Phase 7 改动] 删掉同步 LlmPort.generateReport()
  │   改为发 MQ:
  │   RocketMQ.send("recite-report-topic",
  │     {userId, sessionId, recordIds: [1,2,3...]})
  │
  └─ return {message: "报告生成中", sessionId}


       ═══════════ MQ 异步边界 ═══════════


RocketMQ Consumer 收到消息
  │
  ▼
ReportGenerateConsumer.onMessage(msg)      ← recite-infrastructure/adapter/mq/
  │
  ├─① 查本轮背诵记录
  │   ReciteRecordPort.findBySessionId(userId, sessionId)
  │   └→ SELECT * FROM recite_records WHERE user_id=? AND session_id=?
  │      (report 子域对 recite_records 的只读访问)
  │
  ├─② 查历史学习档案（注入上下文）
  │   ReportPort.findRecentJournals(userId, 5)
  │   └→ SELECT summary_json FROM learning_journal
  │       WHERE user_id=? ORDER BY created_at DESC LIMIT 5
  │
  ├─③ Java 汇总统计（45%）
  │   - 总分 / 平均分 / 题数
  │   - 按模块分组：每题得分
  │   - 识别优势(均分≥7)、薄弱(均分≤4)
  │   - 与前 5 次报告对比趋势（上升/平稳/下降）
  │   - 提取薄弱标签（低分题目高频 tags）
  │
  ├─④ LLM 生成评语（55%）
  │   LlmPort.generateReport(records, recentJournals)
  │   │
  │   └→ Prompt:
  │       "你是学习顾问。根据以下背诵记录和最近学习档案生成报告。
  │        【本轮记录】模块:xxx 得分:8 / 模块:yyy 得分:4 ...
  │        【最近5次档案】{journal1}, {journal2}, ...
  │        返回 JSON:
  │        {summary:"一句话总结",
  │         strengths:["优势1","优势2"],
  │         weaknesses:["薄弱1"],
  │         advice:"综合建议",
  │         trendComment:"趋势分析",
  │         weakTags:["标签1"],
  │         moduleScores:[{moduleKey, moduleName, avgScore, count}]}"
  │
  │   最近 5 次档案注入让 LLM 能看到历史趋势和进步
  │
  ├─⑤ 存入 learning_journal
  │   ReportPort.save(journal)
  │   └→ INSERT INTO learning_journal (user_id, session_id, summary_json)
  │       summary_json = LLM 返回的结构化 JSON（上面的格式）
  │
  └─⑥ 完成


       ═══════════ MQ 异步边界 ═══════════


前端轮询（每 2 秒）:
  │
  ▼
GET /report/{sessionId}

ReportController.getBySessionId(sessionId)
  │
  ├─ ReportPort.findBySessionId(sessionId)
  │   └→ SELECT * FROM learning_journal WHERE session_id=?
  │
  ├─ 返回 null:
  │   └→ Response.ok({status: "generating"})      → 前端 2 秒后重试
  │
  └─ 返回报告:
      └→ Response.ok({status: "done", journal: {...}}) → 前端展示
```

---

### 功能 2：个人仪表盘

**做什么**：用户个人页面展示学习概览——统计卡、模块条、趋势、薄弱标签、AI 建议。

**完整调用链路**：

```
GET /report/dashboard

ReportController.dashboard()
  │  UserContext.getUserId() → userId
  │
  ▼
ReportService.aggregateDashboard(userId)
  │
  ├─① 4 栏统计卡
  │   调 ProgressPort.findByUserId(userId)
  │     → 累计背诵题数 = sum(reciteCount)
  │     → 平均掌握度    = avg(masteryScore)
  │   调 StreakPort.findByUserId(userId)
  │     → 连续天数      = currentStreak
  │   调 ReportPort.countByUserId(userId)
  │     → 报告次数      = count(*)
  │
  ├─② 模块横向条
  │   按 moduleKey 分组统计 avg(masteryScore)
  │   └→ ProgressPort 数据已包含 moduleKey
  │
  ├─③ 趋势数据（最近 7 次）
  │   ReportPort.findRecentJournals(userId, 7)
  │   └→ 提取每次 averageScore → 用于前端折线图
  │
  ├─④ 薄弱标签
  │   从最近 5 次报告的 weakTags 汇总去重
  │
  └─⑤ 最新 AI 建议
      最近一次报告的 advice 字段
```

### 功能 3：学习档案列表

**做什么**：查看历次报告摘要列表 + 查看单次报告详情。

```
GET /report/journal?page=1&size=10

ReportController.journal(page, size)
  └→ ReportPort.findJournals(userId, page, size)
      └→ SELECT id, created_at, summary_json->>'summary' AS summary
          FROM learning_journal WHERE user_id=?
          ORDER BY created_at DESC LIMIT ? OFFSET ?

GET /report/journal/{id}

ReportController.journalDetail(id)
  └→ ReportPort.findById(id)
      └→ SELECT * FROM learning_journal WHERE id=?
          返回完整 summary_json（模块分数、优势、薄弱、建议）
```

---

## 三、涉及存储

| 存储 | Key/表 | 用途 | 读写 |
|---|---|---|---|
| PostgreSQL | `learning_journal` | 报告归档 | 写+读 |
| PostgreSQL | `recite_records` | 消费者读取本轮记录 | 只读（跨子域） |
| PostgreSQL | `user_progress` | 仪表盘模块统计 | 只读（跨子域） |
| PostgreSQL | `user_streak` | 仪表盘连续天数 | 只读（跨子域） |
| RocketMQ | `recite-report-topic` | 报告异步消息 | 写（recite→report） |
| DeepSeek API | — | LLM 评语生成 | — |

---

## 四、类清单与关系

```
┌──────────────────────────────────────────────────────────────────┐
│                     recite-api                                    │
│                                                                   │
│  IReportService     ← /report/dashboard, /report/journal,         │
│                          /report/{sessionId}                      │
│  DTO × 4            ← DashboardDTO, JournalItemDTO,              │
│                        JournalDetailDTO, ReportStatusDTO          │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│                   recite-trigger                                  │
│                                                                   │
│  ReportController  ← 实现 IReportService                          │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│               recite-domain/report (领域层)                        │
│                                                                   │
│  model/entity/                                                    │
│  └─ LearningJournal        学习档案实体                            │
│                                                                   │
│  model/event/                                                     │
│  └─ ReportRequestMessage   MQ 消息体（仅数据，无行为）             │
│                                                                   │
│  port/out/                                                        │
│  ├─ ReportPort             学习档案 CRUD                           │
│  ├─ ReciteRecordPort       ← Phase 5 recite（复用）
│  ├─ ProgressPort           ← Phase 6 progress（复用）
│  ├─ StreakPort             ← Phase 6 progress（复用）
│  └─ LlmPort                ← Phase 5 recite（generateReport）
│                                                                   │
│  service/                                                         │
│  └─ ReportService          仪表盘聚合 + 报告编排                   │
│                                                                   │
│  exception/                                                       │
│  └─ ReportException                                               │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│             recite-infrastructure/adapter                         │
│                                                                   │
│  persistence/                                                     │
│  ├─ ReportPersistenceAdapter   implements ReportPort              │
│  │    └→ LearningJournalDO + LearningJournalMapper               │
│                                                                   │
│  mq/                                                              │
│  └─ ReportGenerateConsumer     RocketMQ 消费者                     │
│       └→ @RocketMQMessageListener(topic="recite-report-topic")    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 五、每个类/接口的详细职责

### 5.1 实体

#### LearningJournal
```java
// 映射 learning_journal 表
Long id;
Long userId;
String sessionId;
String summaryJson;      // JSONB 字段，存 LLM 返回的结构化报告
// summaryJson 内容:
// { totalScore, averageScore, totalQuestions,
//   summary:"一句话总结",
//   strengths:[], weaknesses:[],
//   advice, trendComment, weakTags:[],
//   moduleScores: [{moduleKey, moduleName, avgScore, count}] }
LocalDateTime createdAt;
```

### 5.2 消息体

```java
// MQ 消息，仅数据
public class ReportRequestMessage {
    Long userId;
    String sessionId;
    List<Long> recordIds;
}
```

### 5.3 Port 接口

```java
// --- 本子域 Port ---
public interface ReportPort {
    void save(LearningJournal journal);
    LearningJournal findBySessionId(String sessionId);
    LearningJournal findById(Long id);
    List<LearningJournal> findRecentJournals(Long userId, int limit);
    List<LearningJournal> findJournals(Long userId, int page, int size);
    int countByUserId(Long userId);
}

// --- 跨子域只读 Port ---
// ★ 跨子域 Port 全部复用，不重复定义：
//   - ReciteRecordPort (Phase 5) — 查 recite_records
//   - ProgressPort      (Phase 6) — 查 user_progress
//   - StreakPort        (Phase 6) — 查 user_streak
//   - LlmPort           (Phase 5) — generateReport()
//   Report 子域直接注入这些接口，复用已有实现类。



```

### 5.4 领域服务

```java
public class ReportService {

    // --- 仪表盘聚合 ---
    DashboardDTO aggregateDashboard(Long userId)
    // → 调 ProgressPort(Phase 6) + StreakPort(Phase 6) + ReportPort(本子域)
    // → Java 拼出 4 卡 + 模块条 + 趋势 + 薄弱标签 + 最新建议

    // --- 报告生成（消费者调用） ---
    LearningJournal generateReport(Long userId, String sessionId)
    // → 调 ReciteRecordPort(Phase 5) 查本轮记录
    // → Java 汇总统计
    // → 调 ReportPort.findRecentJournals() 查历史上下文
    // → 调 LlmPort.generateReport() 生成评语
    // → 构建 LearningJournal → ReportPort.save()
}
```

### 5.5 REST 接口

`IReportService`，路径 `/report`：

| # | 方法 | HTTP | 路径 | 说明 |
|:--:|------|------|------|------|
| 1 | `dashboard` | GET | `/dashboard` | 个人仪表盘 |
| 2 | `getBySessionId` | GET | `/{sessionId}` | 轮询报告状态 |
| 3 | `journal` | GET | `/journal?page=1&size=10` | 档案列表 |
| 4 | `journalDetail` | GET | `/journal/{id}` | 单次详情 |

### 5.6 DTO

| DTO | 字段 | 用途 |
|---|---|---|
| `DashboardDTO` | statsCard:{totalQuestions, avgScore, streakDays, reportCount}, moduleBars:[], trend:[{date,avgScore}], weakTags:[], latestAdvice | 仪表盘 |
| `JournalItemDTO` | id, createdAt, summary | 档案列表 |
| `JournalDetailDTO` | id, sessionId, totalScore, averageScore, totalQuestions, strengths, weaknesses, advice, trendComment, weakTags, moduleScores, createdAt | 单次详情 |
| `ReportStatusDTO` | status("generating"/"done"), journal(JournalDetailDTO, null 时表示未生成) | 轮询响应 |

### 5.7 MQ 消费者

```java
@Component
@RocketMQMessageListener(
    topic = "recite-report-topic",
    consumerGroup = "recite-report-consumer"
)
public class ReportGenerateConsumer implements RocketMQListener<ReportRequestMessage> {

    @Autowired
    private ReportService reportService;

    @Override
    public void onMessage(ReportRequestMessage msg) {
        reportService.generateReport(msg.userId, msg.sessionId);
        // 异常时 RocketMQ 自动重试（默认 16 次）
    }
}
```

### 5.8 基础设施

| 适配器 | 实现 | 技术 |
|---|---|---|
| `ReportPersistenceAdapter` | `ReportPort` | MyBatis Plus，操作 `learning_journal` |
| `ReciteRecordAdapter`(Phase 5) | `ReciteRecordPort` | MyBatis Plus 只读，操作 `recite_records` |
| `ProgressAdapter`(Phase 6) | `ProgressPort` | MyBatis Plus 只读，操作 `user_progress` |
| `StreakAdapter`(Phase 6) | `StreakPort` | MyBatis Plus 只读，操作 `user_streak` |
| `ReportGenerateConsumer` | RocketMQ Listener | 消费 `recite-report-topic` |

---

## 六、与 Phase 5 的配合改动

Phase 5 的 `ReciteOrchestrationService.finishRecite()` 需做以下调整：

```
finishRecite 原有逻辑:
  ├─ 查 recite_records
  ├─ Java 基础统计
  ├─ [删除] LlmPort.generateReport()   ← 改为 MQ 异步
  ├─ session → FINISHED
  └─ return SessionReportVO

finishRecite 新逻辑:
  ├─ 查 recite_records
  ├─ Java 基础统计（总分、均分、模块分布）← 保留，前端可即时看基础数据
  ├─ [新增] RocketMQ.send("recite-report-topic", message)
  ├─ session → FINISHED
  └─ return {baseStats: {total, avg, count}, sessionId}
      前端先展示基础数字，然后轮询 /report/{sid} 拿 LLM 评语
```

改动点：Phase 5 删掉 finishRecite 中的 `LlmPort.generateReport()`，改为发送 MQ 消息。这需要 Phase 5 能访问 RocketMQ（Phase 5 的 infrastructure 已依赖 RocketMQ starter）。

---

## 七、文件清单

```
recite-v2/
│
├── recite-api/src/main/java/cn/bugstack/recite/api/
│   ├── IReportService.java
│   └── dto/
│       ├── DashboardDTO.java
│       ├── JournalItemDTO.java
│       ├── JournalDetailDTO.java
│       └── ReportStatusDTO.java
│
├── recite-domain/src/main/java/cn/bugstack/recite/domain/report/
│   ├── model/entity/
│   │   └── LearningJournal.java
│   ├── model/event/
│   │   └── ReportRequestMessage.java
│   ├── port/out/
│   │   ├── ReportPort.java
│   ├── service/
│   │   └── ReportService.java
│   └── exception/
│       └── ReportException.java
│
├── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/adapter/
│   ├── persistence/
│   │   ├── LearningJournalDO.java
│   │   ├── LearningJournalMapper.java
│   │   ├── ReportPersistenceAdapter.java
│   └── mq/
│       └── ReportGenerateConsumer.java
│
└── recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/
    └── ReportController.java
```

**总计 ~21 个文件**（5 api + 9 domain + 6 infra + 1 trigger）

---

## 八、编码顺序

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `ReportException.java` | 其他类依赖 |
| 2 | `LearningJournal.java` `ReportRequestMessage.java` | 数据结构 |
| 3 | 5 个 Port 接口 | 领域契约 |
| 4 | `ReportService.java` | 领域服务 |
| 5 | DTO（4 个）+ `IReportService.java` | API 契约 |
| 6 | DO + Mapper + ReportPersistenceAdapter（2 个，不含跨子域复用） | 持久层 |
| 7 | `ReportGenerateConsumer.java` | MQ 消费者 |
| 8 | `ReportController.java` | 控制器 |
| 9 | Phase 5 finishRecite 改动 | 删同步 LLM，加 MQ 发送 |

每步完成后 `mvn compile` 验证。
