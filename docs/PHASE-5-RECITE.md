# Phase 5 — recite 背诵核心 实施计划

> 日期：2026-06-16 | 状态：计划已审批，待编码

---

## 一、子域定位

recite 是**核心域**，也是**编排中心**。负责背诵会话的完整生命周期。是唯一可以跨子域调用的子域——同步调 knowledge 的 `QuestionPort` 出题，异步发 MQ 给 progress/report/achievement。

---

## 二、要实现哪些功能，怎么实现

### 功能 1：开始背诵（`startRecite`）

**做什么**：用户选模式 → 拉题 → 创建会话 → 返回首题

**怎么实现**：

```
POST /recite/start   {mode, moduleKeys, count}
         │
         ├─① 根据模式拉题（调 QuestionPort，Phase 3 的 knowledge 子域接口）
         │
         │   CATEGORY: QuestionPort.searchByModule(moduleKey, count)
         │   → 从 question_vectors 表按指定模块取 count 道题
         │
         │   RANDOM:  QuestionPort.search("", moduleKeys, count)
         │   → 跨模块随机出题
         │
         │   REVIEW:  查 user_progress 表 WHERE next_review_at <= NOW()
         │   → 只出用户背过的题，固定 10 题，不出新题
         │
         ├─② 创建 ReciteSession 对象
         │   sessionId=UUID, userId, mode, questionIds[题目列表],
         │   currentIndex=0, status=ACTIVE, followUpDepth=0
         │
         ├─③ 存入 Redis
         │   Key: recite:session:{sid}
         │   Value: ReciteSession 序列化为 JSON
         │   TTL: 2 小时
         │
         └─④ 返回 {sessionId, 首题内容, 当前第1题/共N题}
```

---

### 功能 2：提交答案 + SSE 流式评分（`submitAnswer`）

**做什么**：用户输入答案 → 调 DeepSeek 评分 → 分 6 段推送给前端 → 存记录

**怎么实现**：

```
POST /recite/{sid}/answer   {questionId, answer}

  前端收到的是 SSE 流，不是普通 JSON 响应。
  后端用 Spring SseEmitter + 线程池异步执行。

  ┌─ Controller 层 ─────────────────────────────────────┐
  │                                                     │
  │  SseEmitter emitter = new SseEmitter(60_000L);     │
  │  taskExecutor.execute(() -> {                       │
  │      // 所有业务逻辑在线程池中执行                    │
  │      // 每步完成后 emitter.send(...) 推送一段        │
  │  });                                                │
  │  return emitter;  ← 立即返回，HTTP 连接保持          │
  └─────────────────────────────────────────────────────┘

  线程池内的执行步骤：

  ├─① 校验
  │   从 Redis 读 session → 校验 userId 归属（防止越权）
  │   → 校验答案非空且 ≤5000 字
  │
  ├─② 获取评分槽位（并发控制）
  │   ScoreSlotPort.tryAcquire(30s)
  │   │
  │   └→ Redis Semaphore: recite:score:slots
  │      共 10 个 permit
  │      10 人同时评分 → 满，第 11 人阻塞等待
  │      等 30 秒仍未获取 → 抛异常，提示"系统繁忙"
  │
  ├─③ 调 LLM 评分
  │   LlmPort.score(question, answer)
  │   │
  │   └→ OkHttp POST https://api.deepseek.com/v1/chat/completions
  │      Prompt: "你是大厂校招面试官。根据题目和回答评分..."
  │      要求返回 JSON: {score:1-10, correctPoints:[], missedPoints:[],
  │                      suggestion:"", followUpQuestion:""}
  │      耗时约 2-5 秒
  │
  ├─④ 释放槽位
  │   ScoreSlotPort.release()
  │   → Redis Semaphore permit +1，下一个人可以进入
  │
  ├─⑤ 持久化记录
  │   ReciteRecordPort.save(record)
  │   → INSERT recite_records (user_id, session_id, question_id,
  │        module_key, user_answer, score, feedback,
  │        response_time_seconds, ...)
  │
  ├─⑥ SSE 分 6 段推送（每段间隔 400ms）
  │
  │   段1 [0ms]:    {"type":"score",       "score":8}
  │   段2 [400ms]:  {"type":"correct",     "points":["提到了A","提到了B"]}
  │   段3 [800ms]:  {"type":"missed",      "points":["遗漏了C"]}
  │   段4 [1200ms]: {"type":"suggestion",  "text":"建议补充D..."}
  │   段5 [1600ms]: {"type":"followUp",    "question":"追问：请深入解释..."}
  │   段6 [2000ms]: {"type":"done",        "recordId":42,
  │                                         "nextIndex":3, "total":10}
  │
  │   前端 EventSource 逐段接收：
  │   - 段1 分数先出现（数字跳动动画）
  │   - 段2-3 正确点/遗漏点逐条列出
  │   - 段4 建议文字出现
  │   - 段5 追问弹出（带"接受追问"/"跳过"按钮）
  │   - 段6 标记完成，可以点"下一题"
  │
  └─⑦ 更新 Redis session
       currentIndex + 1 → 指向下一题
```

---

### 功能 3：追问链（`submitFollowUp`）

**做什么**：评分后 LLM 生成了一个追问，用户选择回答。追问不在题库中，不评分，不进 user_progress。

**怎么实现**：

```
POST /recite/{sid}/followup   {recordId, followUpAnswer}
         │
         ├─① 校验
         │   从 Redis 读 session → 校验 userId
         │   → 校验 followUpDepth < 3（最多 3 层追问，超过拒绝）
         │
         ├─② 调 LLM 追问反馈
         │   LlmPort.followUp(question, answer)
         │   → Prompt: "根据回答提出一个深挖的追问..."
         │   → 返回追问反馈文字（仅用于帮助回忆，不评分）
         │
         ├─③ 更新数据库
         │   recite_records 表对应记录：
         │   UPDATE SET follow_up_answer=?, follow_up_feedback=?
         │
         └─④ 更新 Redis session
              followUpDepth + 1

  约束：
  - 追问题目由 LLM 现场生成，不在 question_vectors 表中
  - 追问回答不评分，不写入 user_progress
  - 最多 3 层，每层需用户主动点"接受追问"才发起
```

---

### 功能 4：结束背诵（`finishRecite`）

**做什么**：用户点击结束 → Java 统计数字 + LLM 写评语 → 返回报告

**怎么实现**：

```
POST /recite/{sid}/finish
         │
         ├─① 查全部记录
         │   ReciteRecordPort.findBySessionId(userId, sessionId)
         │   → 返回本轮所有 recite_records
         │
         ├─② Java 汇总数字（45% 工作量）
         │   - 总分 sum(scores)
         │   - 平均分 avg(scores)
         │   - 按模块分组统计
         │   - 标记优势模块（均分 ≥7）
         │   - 标记薄弱模块（均分 ≤4）
         │
         ├─③ 发 MQ 消息（异步报告 + 成就评估）
         │   RocketMQTemplate.send("recite-report-topic",
         │     {userId, sessionId, recordIds: [...]})
         │   RocketMQTemplate.send("recite-achievement-topic",
         │     {userId, sessionId})
         │   → fire-and-forget，Phase 7 消费者异步生成报告
         │   → fire-and-forget，Phase 8 消费者异步评估徽章
         │
         ├─④ 标记完成
         │   session.status = FINISHED → 写回 Redis
         │
         └─⑤ 立即返回（不等报告和成就）
              {baseStats: {totalScore, averageScore, totalQuestions}, sessionId}
              前端先展示基础统计，然后轮询 /report/{sid} 和 /achievement/new

> LlmPort.generateReport() 已从此处移除。Phase 7 的 ReportGenerateConsumer 中调用。Phase 5 只发 MQ 消息。

---

### 功能 5：查看历史（`getHistory`）

```
GET /recite/history?limit=20
   → ReciteRecordPort.findByUserId(userId, limit)
   → 返回最近 20 条背诵记录
```

---

### 功能 6：查询会话状态（`getSession`）

```
GET /recite/{sid}
   → 从 Redis 读 session
   → 返回 {sessionId, mode, currentIndex, totalQuestions, status}
   → 前端用这个显示进度条（第X题/共Y题）
```

---

## 三、涉及存储

| 存储 | 用途 | Key / 表 | 过期 |
|---|---|---|---|
| Redis | 会话状态 | `recite:session:{sid}` | 2h |
| Redis | 评分并发槽 | `recite:score:slots` (Semaphore 10) | — |
| PostgreSQL | 背诵记录 | `recite_records` | — |
| PostgreSQL | 掌握度 | `user_progress` | — |
| DeepSeek API | 评分、追问、报告 | `api.deepseek.com` | — |

---

## 四、类清单与关系

```
┌──────────────────────────────────────────────────────────────────┐
│                     recite-api (REST 契约)                        │
│                                                                   │
│  IReciteService     ← 6 个 REST 端点                              │
│  DTO × 7            ← 请求/响应 传输对象                           │
│  (SseEmitter 是 Spring 内置类，不自定义)                          │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│                   recite-trigger                                  │
│                                                                   │
│  ReciteController                                                 │
│   ─ 实现 IReciteService                                           │
│   ─ submitAnswer 方法返回 SseEmitter                              │
│   ─ 注入线程池 taskExecutor，SSE 推送在异步线程执行                │
│   ─ 依赖 UserContext.getUserId() 获取当前用户                      │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│               recite-domain/recite (领域层)                        │
│                                                                   │
│  model/entity/                                                    │
│  ├─ ReciteSession         会话（存 Redis 的 JSON 对象）            │
│  └─ ReciteRecordEntity    背诵记录（存 PG）                        │
│                                                                   │
│  model/valueobj/                                                  │
│  ├─ ScoreResultVO         record: LLM 评分结果                    │
│  └─ SessionReportVO       record: 会话报告汇总                    │
│                                                                   │
│  port/out/ (SPI — infra 实现)                                     │
│  ├─ ReciteSessionPort    Redis 会话读写                           │
│  ├─ ScoreSlotPort        Redis 评分信号量                         │
│  ├─ LlmPort              DeepSeek 评分/追问/报告                  │
│  └─ ReciteRecordPort     recite_records 持久化                    │
│                                                                   │
│  外部依赖（其他子域的 Port）:                                      │
│  └─ QuestionPort         ← Phase 3 knowledge，出题唯一入口         │
│                                                                   │
│  service/                                                         │
│  ├─ ReciteOrchestrationService  核心编排（不调外部 API）            │
│  └─ ReciteGateService           门控校验（纯规则）                 │
│                                                                   │
│  exception/                                                       │
│  └─ ReciteException     会话不存在/越权/槽满/追问超层              │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│             recite-infrastructure/adapter                         │
│                                                                   │
│  cache/                                                           │
│  ├─ ReciteSessionAdapter   implements ReciteSessionPort           │
│  │    └→ Redisson RBucket<ReciteSession>, Jackson序列化, 2h TTL  │
│  └─ ScoreSlotAdapter       implements ScoreSlotPort               │
│       └→ Redisson RSemaphore("recite:score:slots"), 10 permits   │
│                                                                   │
│  llm/                                                             │
│  └─ DeepSeekLlmAdapter     implements LlmPort                     │
│       └→ OkHttp → POST api.deepseek.com/v1/chat/completions      │
│       └→ Prompt 模板 + JSON 解析（extractJson 去 markdown 包裹）  │
│                                                                   │
│  persistence/                                                     │
│  └─ ReciteRecordAdapter   implements ReciteRecordPort             │
│       └→ ReciteRecordDO + ReciteRecordMapper (MyBatis Plus)       │
└──────────────────────────────────────────────────────────────────┘
```

---

## 五、每个类/接口的详细职责

### 5.1 领域实体

#### ReciteSession（Redis 存储对象）
```java
public class ReciteSession {
    String sessionId;           // UUID
    Long userId;                // 所属用户
    ReciteMode mode;            // CATEGORY / RANDOM / REVIEW
    List<String> moduleKeys;    // 模块范围
    String currentQuestionId;   // 当前题目 ID
    List<String> questionIds;   // 本次全部题目（startRecite 时一次拉取）
    int currentIndex;           // 当前题目序号，0-based
    int totalQuestions;         // 总题数
    String status;              // ACTIVE / FINISHED
    int followUpDepth;          // 当前追问层数 0-3
    LocalDateTime createdAt;
}
```

#### ReciteRecordEntity（PG 存储实体）
```java
public class ReciteRecordEntity {
    Long id;
    Long userId;
    String sessionId;
    String mode;               // CATEGORY / RANDOM / REVIEW
    String moduleKey;
    String questionId;
    String userAnswer;
    Integer score;             // 1-10，REVIEW 模式为 null
    String feedback;           // correctPoints 拼接
    String followUpQuestion;   // LLM 生成的追问
    String followUpAnswer;     // 用户追问回答
    String followUpFeedback;   // 追问反馈
    int followUpDepth;         // 追问层数
    Long parentRecordId;       // 追问链父记录 ID
    int responseTimeSeconds;   // 单题答题耗时
    LocalDateTime createdAt;
}
```

### 5.2 值对象

```java
// LLM 评分返回
public record ScoreResultVO(
    Integer score,                    // 1-10
    List<String> correctPoints,       // 正确点
    List<String> missedPoints,        // 遗漏点
    String suggestion,                // 改进建议
    String followUpQuestion           // 追问题目（可空）
) {}

// 会话报告
public record SessionReportVO(
    Double totalScore,
    Double averageScore,
    Integer totalQuestions,
    List<String> strengths,           // 优势模块
    List<String> weaknesses,          // 薄弱模块
    String advice                     // LLM 综合评语
) {}
```

### 5.3 Port 接口（SPI）

#### ReciteSessionPort
```java
public interface ReciteSessionPort {
    void save(ReciteSession session);                   // 写入 Redis，TTL 2h
    Optional<ReciteSession> findById(String sessionId); // 读取
    void update(ReciteSession session);                 // 更新（保持原有 TTL）
    void delete(String sessionId);                      // 删除
    void refreshTtl(String sessionId);                  // 每次操作续期 2h
}
```

#### ScoreSlotPort
```java
public interface ScoreSlotPort {
    boolean tryAcquire(long timeoutMs);   // 尝试获取评分槽位，超时返回 false
    void release();                       // 释放槽位
    int available();                      // 剩余槽位数
}
```

#### LlmPort
```java
public interface LlmPort {
    ScoreResultVO score(QuestionEntity question, String userAnswer);
    String followUp(QuestionEntity question, String userAnswer);
    SessionReportVO generateReport(List<ReciteRecordEntity> records);
}
```

#### ReciteRecordPort
```java
public interface ReciteRecordPort {
    ReciteRecordEntity save(ReciteRecordEntity record);
    void updateFollowUp(Long recordId, String answer, String feedback);
    List<ReciteRecordEntity> findBySessionId(Long userId, String sessionId);
    List<ReciteRecordEntity> findByUserId(Long userId, int limit);
    ReciteRecordEntity findById(Long recordId);

    // ---- M-eM-8M-8M-eM-^PM-^HM-gM-;M-^_M-hM-.M-!M-fM-^UM-0M-oM-<M-^HM-gM-^TM-( Phase 7/8 M-hM-7M-(M-eM-^_M-^_M-hM-^CM-=M-hM-/M-"M-oM-<M-^I ----
    int countByUserId(Long userId);
    double avgScoreByUserId(Long userId);
    int countByModule(Long userId, String moduleKey);
    int countPerfectScores(Long userId);
    int countSessionsByUserId(Long userId);
    int lastSessionHour(Long userId);
    int lastSessionMaxFollowUpDepth(Long userId);
    int lastSessionPerfectCount(Long userId);
    int lastSessionQuestionCount(Long userId);
    int lastSessionMinAnswerSeconds(Long userId);
}
```

### 5.4 领域服务

#### ReciteOrchestrationService

核心编排，纯业务逻辑，不调外部 API（全部通过 Port 调用）：

```java
public class ReciteOrchestrationService {

    // 注入
    QuestionPort questionPort;         // Phase 3 knowledge
    ReciteSessionPort sessionPort;     // Redis
    ScoreSlotPort scoreSlotPort;       // Redis Semaphore
    LlmPort llmPort;                   // DeepSeek
    ReciteRecordPort recordPort;       // PG
    ProgressPort progressPort;         // Phase 6 progress
    RocketMQTemplate rocketMQTemplate; // MQ M-eM-^OM-^QM-eM-8M-^C

    // ---- 功能 1: 开始背诵 ----
    ReciteSession startRecite(Long userId, ReciteMode mode,
                              List<String> moduleKeys, int count)
    // → 按模式拉题 → 创建 session → 存 Redis → 返回 session

    // ---- 功能 2: 提交答案 ----
    ScoreResultVO submitAnswer(Long userId, String sessionId,
                               String questionId, String answer)
    // → 校验 session → 抢槽位 → LLM 评分 → 释放槽位 → 存记录 → 更新 session → 返回结果

    // ---- 功能 3: 追问 ----
    String submitFollowUp(Long userId, String sessionId,
                          Long recordId, String followUpAnswer)
    // → 校验深度 < 3 → LLM 追问 → 更新记录 → session.followUpDepth++

    // ---- 功能 4: 结束 ----
    SessionReportVO finishRecite(Long userId, String sessionId)
    // → 查全部记录 → Java汇总数字 → 发MQ(报告+成就) → session=FINISHED → 立即返回
}
```

#### ReciteGateService

门控校验（纯规则，无外部依赖）：

```java
public class ReciteGateService {
    void validateSession(ReciteSession session, Long userId);
    // session 存在 + userId 匹配 + status != FINISHED

    void validateAnswer(String answer);
    // 非空 + 长度 ≤ 5000

    void validateScore(int score);
    // 1 ≤ score ≤ 10

    void validateFollowUpDepth(int depth);
    // depth < 3
}
```

### 5.5 REST 接口（`IReciteService`）

路径前缀 `/recite`，所有端点需要登录（UserContextInterceptor 校验）：

| # | 方法 | HTTP | 路径 | 请求体 | 返回 |
|:--:|------|------|------|------|------|
| 1 | `startRecite` | POST | `/start` | `ReciteStartRequestDTO` | `Response<ReciteStartResultDTO>` |
| 2 | `submitAnswer` | POST | `/{sid}/answer` | `SubmitAnswerRequestDTO` | **`SseEmitter`**（SSE 流） |
| 3 | `submitFollowUp` | POST | `/{sid}/followup` | `FollowUpRequestDTO` | `Response<String>` |
| 4 | `finishRecite` | POST | `/{sid}/finish` | — | `Response<SessionReportDTO>` |
| 5 | `getSession` | GET | `/{sid}` | — | `Response<ReciteSessionDTO>` |
| 6 | `getHistory` | GET | `/history?limit=20` | — | `Response<List<ReciteRecordDTO>>` |

### 5.6 DTO（7 个）

| DTO | 字段 | 用途 |
|---|---|---|
| `ReciteStartRequestDTO` | mode, moduleKeys, count | 开始背诵请求 |
| `ReciteStartResultDTO` | sessionId, question (QuestionDTO), questionIndex, totalQuestions | 开始背诵响应 |
| `SubmitAnswerRequestDTO` | questionId, answer | 提交答案请求 |
| `FollowUpRequestDTO` | recordId, followUpAnswer | 追问请求 |
| `SessionReportDTO` | totalScore, averageScore, totalQuestions, strengths, weaknesses, advice | 报告响应 |
| `ReciteSessionDTO` | sessionId, mode, currentIndex, totalQuestions, status | 会话状态响应 |
| `ReciteRecordDTO` | id, sessionId, mode, questionTitle, score, createdAt | 历史记录响应 |

### 5.7 基础设施适配器

| 适配器 | 实现接口 | 技术要点 |
|---|---|---|
| `ReciteSessionAdapter` | `ReciteSessionPort` | Redisson `RBucket`，Jackson 序列化为 JSON，`expire(2, HOURS)` |
| `ScoreSlotAdapter` | `ScoreSlotPort` | Redisson `RSemaphore("recite:score:slots")`，初始化 10 permits |
| `DeepSeekLlmAdapter` | `LlmPort` | OkHttp 30s 连接超时/120s 读取超时，Prompt 模板 + `extractJson()` 解析 |
| `ReciteRecordAdapter` | `ReciteRecordPort` | MyBatis Plus `LambdaQueryWrapper`，操作 `recite_records` 表 |

### 5.8 持久层

| 类 | 映射表 | 说明 |
|---|---|---|
| `ReciteRecordDO` | `recite_records` | MyBatis Plus PO，字段含新增的 `follow_up_depth`、`parent_record_id`、`response_time_seconds` |
| `ReciteRecordMapper` | `recite_records` | `extends BaseMapper<ReciteRecordDO>` |

---

## 六、SSE 评分完整时序

```
POST /recite/{sid}/answer
  │
  ├─ Controller 立即返回 SseEmitter（60s 超时），连接保持
  │
  ├─ [线程池线程 ThreadPoolTaskExecutor]
  │   │
  │   ├─ 1. ReciteGateService.validateSession()
  │   ├─ 2. ReciteGateService.validateAnswer()
  │   ├─ 3. scoreSlotPort.tryAcquire(30_000)
  │   │      ↓ 获取成功
  │   ├─ 4. llmPort.score(question, answer)     ← DeepSeek API ~2-5s
  │   ├─ 5. scoreSlotPort.release()
  │   ├─ 6. recordPort.save(record)             ← INSERT recite_records
  │   │
  │   ├─ 7. emitter.send( SseEmitter.event().name("score")    .data({score:8}) )        [0ms]
  │   ├─ 8. Thread.sleep(400)
  │   ├─ 9. emitter.send( SseEmitter.event().name("correct")  .data({points:[...]}) )   [400ms]
  │   ├─10. Thread.sleep(400)
  │   ├─11. emitter.send( SseEmitter.event().name("missed")   .data({points:[...]}) )   [800ms]
  │   ├─12. Thread.sleep(400)
  │   ├─13. emitter.send( SseEmitter.event().name("suggestion").data({text:"..."}) )    [1200ms]
  │   ├─14. Thread.sleep(400)
  │   ├─15. emitter.send( SseEmitter.event().name("followUp") .data({question:"..."}) ) [1600ms]
  │   ├─16. Thread.sleep(400)
  │   ├─17. emitter.send( SseEmitter.event().name("done")     .data({recordId:42}) )    [2000ms]
  │   └─18. emitter.complete()
  │
  └─ 前端 EventSource
       ├─ addEventListener("score",      e → 分数数字跳动)
       ├─ addEventListener("correct",    e → 正确点逐条出现)
       ├─ addEventListener("missed",    e → 遗漏点标红)
       ├─ addEventListener("suggestion", e → 建议文字淡入)
       ├─ addEventListener("followUp",   e → 追问气泡弹出)
       └─ addEventListener("done",       e → 可点击"下一题")
```

---

## 七、三种背诵模式

| 模式 | 出题方式 | 出题数 | 评分 | 调 LLM |
|------|------|:--:|------|:--:|
| CATEGORY | `QuestionPort.searchByModule(moduleKey, count)` | 用户选 | AI 1-10 分 | ✅ |
| RANDOM | `QuestionPort.search("", moduleKeys, count)` | 用户选 | AI 1-10 分 | ✅ |
| REVIEW | 查 `user_progress` where `next_review_at <= now` | 固定 10 | 自评(想起/不确定/忘了) | ❌ |

---

## 八、依赖注入关系

```
ReciteController
  └─ @Autowired ReciteOrchestrationService
  └─ @Autowired ReciteSessionPort → ReciteSessionAdapter

ReciteOrchestrationService
  ├─ @Autowired QuestionPort          ← Phase 3 knowledge
  ├─ @Autowired ReciteSessionPort     → ReciteSessionAdapter
  ├─ @Autowired ScoreSlotPort         → ScoreSlotAdapter
  ├─ @Autowired LlmPort               → DeepSeekLlmAdapter
  ├─ @Autowired ReciteRecordPort      → ReciteRecordAdapter
  ├─ @Autowired ProgressPort         ← Phase 6 progress
  ├─ @Autowired RocketMQTemplate     ← MQ 发送
  └─ @Autowired ReciteGateService     (同包组件)
```

---

## 九、文件清单

```
recite-v2/
│
├── recite-api/src/main/java/cn/bugstack/recite/api/
│   ├── IReciteService.java
│   └── dto/
│       ├── ReciteStartRequestDTO.java
│       ├── ReciteStartResultDTO.java
│       ├── SubmitAnswerRequestDTO.java
│       ├── FollowUpRequestDTO.java
│       ├── SessionReportDTO.java
│       ├── ReciteSessionDTO.java
│       └── ReciteRecordDTO.java
│
├── recite-domain/src/main/java/cn/bugstack/recite/domain/recite/
│   ├── model/entity/
│   │   ├── ReciteSession.java
│   │   └── ReciteRecordEntity.java
│   ├── model/valueobj/
│   │   ├── ScoreResultVO.java
│   │   └── SessionReportVO.java
│   ├── port/out/
│   │   ├── ReciteSessionPort.java
│   │   ├── ScoreSlotPort.java
│   │   ├── LlmPort.java
│   │   └── ReciteRecordPort.java
│   ├── service/
│   │   ├── ReciteOrchestrationService.java
│   │   └── ReciteGateService.java
│   └── exception/
│       └── ReciteException.java
│
├── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/adapter/
│   ├── cache/
│   │   ├── ReciteSessionAdapter.java
│   │   └── ScoreSlotAdapter.java
│   ├── llm/
│   │   └── DeepSeekLlmAdapter.java
│   └── persistence/
│       ├── ReciteRecordDO.java
│       ├── ReciteRecordMapper.java
│       └── ReciteRecordAdapter.java
│
└── recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/
    └── ReciteController.java
```

**总计 ~26 个文件**（8 api + 12 domain + 5 infra + 1 trigger）

---

## 十、编码顺序

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `ReciteException.java` | 其他类依赖 |
| 2 | `ReciteSession.java` `ReciteRecordEntity.java` | 纯数据结构 |
| 3 | `ScoreResultVO.java` `SessionReportVO.java` | 值对象 |
| 4 | 4 个 Port 接口 | 领域契约 |
| 5 | `ReciteGateService.java` | 纯规则，无外部依赖 |
| 6 | `ReciteOrchestrationService.java` | 核心编排 |
| 7 | DTO（7 个）+ `IReciteService.java` | API 契约 |
| 8 | `ReciteRecordDO` + `ReciteRecordMapper` | 持久层 |
| 9 | `ReciteRecordAdapter` `DeepSeekLlmAdapter` | 适配器 |
| 10 | `ReciteSessionAdapter` `ScoreSlotAdapter` | Redis 适配器 |
| 11 | `ReciteController.java` | 最后组装 |

每步完成后 `mvn compile` 验证。
