# Phase 7 编码报告 — report 报告子域

> 日期：2026-06-18 | 范围：MQ 异步报告 + 仪表盘 + 学习档案 | 编译：BUILD SUCCESS

---

## 一、文件清单

### recite-domain/report（6 个）

| 文件 | 职责 |
|------|------|
| `ReportException.java` | 子域异常 |
| `LearningJournal.java` | 学习档案实体：userId, sessionId, summaryJson(JSONB), createdAt |
| `ReportRequestMessage.java` | MQ 消息体：userId, sessionId, recordIds |
| `ReportPort.java` | SPI：save / findBySessionId / findById / findRecentJournals / findJournals / countByUserId |
| `ReportService.java` | 领域服务：generateReport(消费者调用) + aggregateDashboard(仪表盘) |
| `DashboardVO.java`（ReportService 内部 record）| 仪表盘值对象：StatsCard, TrendPoint, moduleBars, weakTags, latestAdvice |

### recite-api（5 个）

| 文件 | 职责 |
|------|------|
| `IReportService.java` | 4 端点 REST 契约 |
| `DashboardDTO.java` | 仪表盘：统计卡 + 模块条 + 趋势 + 标签 + 建议 |
| `JournalItemDTO.java` | 档案列表项：id, createdAt, summary |
| `JournalDetailDTO.java` | 档案详情：完整 LLM 报告字段 + moduleScores |
| `ReportStatusDTO.java` | 轮询响应：status(generating/done) + journal |

### recite-infrastructure（5 个）

| 文件 | 职责 |
|------|------|
| `LearningJournalDO.java` | 映射 learning_journal 表 |
| `LearningJournalMapper.java` | BaseMapper |
| `ReportPersistenceAdapter.java` | 实现 ReportPort，MyBatis Plus |
| `ReportGenerateConsumer.java` | RocketMQ 消费者，topic=recite-report-topic |
| `ReportMessageAdapter.java` | 实现 ReportMessagePort，RocketMQTemplate 发送 |

### recite-trigger（1 个）

| 文件 | 职责 |
|------|------|
| `ReportController.java` | 实现 IReportService，DTO 映射（含 summaryJson JSONB 解析） |

### 配合改动（3 个修改 + 1 新建）

| 文件 | 改动 |
|------|------|
| `ReportMessagePort.java`（recite-domain，新建）| SPI：sendReportRequest(userId, sessionId, recordIds) |
| `ReciteOrchestrationService.java` | finishRecite：删 LlmPort.generateReport() → 加 Java 基础统计 + ReportMessagePort.sendReportRequest() |
| `LlmPort.java` | 加 generateJournalReport(records, recentJournalSummaries) 方法 |
| `DeepSeekLlmAdapter.java` | 实现 generateJournalReport（含历史档案上下文的 Prompt） |
| `recite-domain/pom.xml` | 加 Gson 依赖（ReportService 解析 JSON） |

**总计 ~18 个新文件 + 4 个修改文件**。

---

## 二、异步报告调用链路

```
POST /recite/{sid}/finish
  ReciteController.finishRecite(sid)
    └─ ReciteOrchestrationService.finishRecite(userId, sid)
         ├─ 查 recite_records → Java 基础统计（总分/均分/模块分布）
         ├─ ReportMessagePort.sendReportRequest() → RocketMQ 异步
         ├─ session.status = FINISHED
         ├─ streakService.checkIn()
         └─ return SessionReportVO（基础数据，"报告生成中"）


       ═══════════ MQ 异步边界 ═══════════


ReportGenerateConsumer.onMessage(msg)
  ├─ ReciteRecordPort.findBySessionId() → 本轮记录
  ├─ ReportPort.findRecentJournals(5) → 历史上下文
  ├─ Java 汇总统计
  ├─ LlmPort.generateJournalReport(records, recentSummaries) → LLM 评语 JSON
  ├─ ReportPort.save(journal) → learning_journal
  └─ 完成


       ═══════════ 前端轮询 ═══════════


GET /report/{sessionId}
  └─ ReportPort.findBySessionId()
       ├─ null → {status:"generating"} → 2 秒后重试
       └─ 有值 → {status:"done", journal:{...}} → 前端展示完整报告
```

---

## 三、仪表盘调用链路

```
GET /report/dashboard
  ReportController.dashboard()
    └─ ReportService.aggregateDashboard(userId)
         ├─ ProgressPort.findByUserId() → 累计题数/平均掌握度
         ├─ StreakPort.findByUserId() → 连续天数
         ├─ ReportPort.countByUserId() → 报告次数
         ├─ ReportPort.findRecentJournals(7) → 趋势
         ├─ 解析 summaryJson → weakTags 去重
         └─ 最新 advice
```

---

## 四、新增数据库表

```sql
CREATE TABLE learning_journal (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    summary_json JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_lj_user ON learning_journal(user_id, created_at DESC);
CREATE UNIQUE INDEX idx_lj_session ON learning_journal(session_id);
```

---

## 五、与 Phase 5 的改动对照

| 原代码 | 改为 |
|------|------|
| `llmPort.generateReport(records)` 同步阻塞 ~5s | `reportMessagePort.sendReportRequest()` fire-and-forget |
| 返回完整 LLM 报告 | 返回 Java 基础统计 + "报告生成中" |
| finishRecite 阻塞 HTTP 线程 | finishRecite 立即返回 |

前端需配合：`finishRecite` 返回后先展示基础数据，然后轮询 `GET /report/{sessionId}` 获取完整 LLM 报告。

---

## 六、编译结果

```
recite-types          SUCCESS
recite-api            SUCCESS
recite-domain         SUCCESS
recite-infrastructure SUCCESS
recite-trigger        SUCCESS
recite-app            SUCCESS
BUILD SUCCESS
```
