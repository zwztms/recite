# Phase 5 编码报告 — recite 背诵核心

> 日期：2026-06-17 | 提交：b0ee80c → 89d7686 | 范围：recite 子域完整实现

---

## 一、总体情况

| 步骤 | 内容 | 新建 | 修改 | 状态 |
|:--:|------|:--:|:--:|:--:|
| 0 | docker-compose + Redis/RocketMQ 配置 | 1 | 1 | ✅ |
| 1 | 领域基础：9 实体/VO/Port + 8 DTO/接口 | 17 | 0 | ✅ |
| 2 | 门控 + 编排骨架 | 2 | 0 | ✅ |
| 3 | 6 个适配器（Redis/DeepSeek/PG） | 6 | 0 | ✅ |
| 5 | startRecite 实现 | 1 | 1 | ✅ |
| 6 | submitAnswer + SSE 流式评分 | 0 | 2 | ✅ |
| 7 | submitFollowUp 追问链 | 0 | 2 | ✅ |
| 8 | finishRecite 结束背诵 | 0 | 2 | ✅ |
| — | getSession + getHistory | (Step 5 顺带) | — | ✅ |
| **合计** | | **27** | **8** | |

---

## 二、各步骤实现细节

### Step 0 — 环境准备

**解决问题**：Phase 5 依赖 Redis（会话存储 + 评分信号量）和 RocketMQ（finishRecite 发消息），项目启动前需要这些中间件。

**实现**：
- `docker-compose.yml`：pgvector/pgvector:pg16(5433) + redis:7-alpine(6379) + apache/rocketmq:5.3.0(9876+10911)
- PG 端口改用 5433 避免与 recite V1 的 5432 冲突
- RocketMQ 健康检查去掉 curl（容器不包含），改为简单启动
- `application-dev.yml`：加 redis/redisson/rocketmq 连接配置

---

### Step 1 — 领域基础（17 文件）

**实现**：

| 模块 | 文件 | 职责 |
|------|------|------|
| domain | `ReciteException` | 子域异常：会话不存在/越权/槽满/追问超层 |
| domain | `ReciteSession` | Redis 会话实体：sessionId, userId, mode, questionIds[], currentIndex, followUpDepth |
| domain | `ReciteRecordEntity` | PG 记录实体：含 followUpQuestion/Answer/Feedback/Depth/parentRecordId |
| domain | `ScoreResultVO` | record: score(1-10), correctPoints[], missedPoints[], suggestion, followUpQuestion |
| domain | `SessionReportVO` | record: totalScore, averageScore, strengths[], weaknesses[], advice |
| domain | `ReciteSessionPort` | SPI: save/findById/update/delete |
| domain | `ScoreSlotPort` | SPI: tryAcquire(timeoutMs)/release/available |
| domain | `LlmPort` | SPI: score(question, answer)/followUp(question, answer)/generateReport(records) |
| domain | `ReciteRecordPort` | SPI: save/updateFollowUp/findBySessionId/findByUserId/findById + 统计方法 |
| api | `IReciteService` | 6 端点 REST 契约 |
| api | DTO × 7 | ReciteStartRequest/Result, SubmitAnswer, FollowUp, SessionReport, ReciteSession, ReciteRecord |

**数据结构特点**：
- 值对象全部用 `record`（不可变），传输安全
- ReciteSession 存 Redis 用 Jackson 序列化（非 PG 实体）
- ReciteRecordEntity 字段对齐 V1 的 recite_records 表结构，新增 followUpDepth、parentRecordId、responseTimeSeconds

---

### Step 2 — 门控 + 编排骨架

**ReciteGateService**（纯规则，零外部依赖）：

| 方法 | 校验内容 | 违反抛 |
|------|---------|--------|
| `validateSession` | session 非空 + userId 匹配 + status≠FINISHED | ReciteException(404/403/400) |
| `validateAnswer` | 非空 + 长度 ≤ 5000 | ReciteException(400) |
| `validateScore` | 1 ≤ score ≤ 10 | ReciteException(500) |
| `validateFollowUpDepth` | depth < 3 | ReciteException(400) |

**ReciteOrchestrationService**（注入 6 个 Port + GateService，方法体先抛 UnsupportedOperationException 占位）。

---

### Step 3 — 6 个适配器

| 适配器 | 实现接口 | 技术 | 关键点 |
|--------|---------|------|--------|
| `ReciteSessionAdapter` | ReciteSessionPort | Redisson RBucket + Jackson + JavaTimeModule | 自动 2h TTL |
| `ScoreSlotAdapter` | ScoreSlotPort | Redisson RSemaphore("recite:score:slots") | 首次 trySetPermits(10) |
| `DeepSeekLlmAdapter` | LlmPort | OkHttp 30s/120s 超时 | Prompt 模板 + extractJson() 去 markdown |
| `ReciteRecordAdapter` | ReciteRecordPort | MyBatis Plus | userId 隔离查询 |
| `ReciteRecordDO` | — | @TableName("recite_records") | 含 followUpDepth/parentRecordId/responseTimeSeconds |
| `ReciteRecordMapper` | — | BaseMapper + 自定义统计 SQL | countByUserId/avgScore/perModule/countPerfect/countSessions |

**DeepSeekLlmAdapter 评分提示词框架**：
```
你是大厂校招面试官。根据题目和回答评分（1-10分）。
题目：%s
参考答案：%s
用户回答：%s
返回JSON: {"score":8,"correctPoints":[...],"missedPoints":[...],"suggestion":"","followUpQuestion":""}
```

**容错处理**：API 异常时 score() 兜底返回 5 分 + 解析异常提示，不中断主流程。

---

### Step 5 — startRecite

**完整调用链**：
```
POST /recite/start  {mode, moduleKeys, count}
  │
  ├─ ReciteController.startRecite()
  │     └─ userId = UserContext.getUserId()
  │     └─ orchestrationService.startRecite(userId, mode, keys, count)
  │     └─ questionPort.getById(session.currentQuestionId) → 首题 DTO
  │
  └─ ReciteOrchestrationService.startRecite()
        └─ CATEGORY → questionPort.searchByModule(key, count)
        └─ RANDOM  → questionPort.search("", moduleKeys, count)
        └─ 生成 sessionId(UUID) → 构建 ReciteSession → sessionPort.save() → Redis
```

**模式分发**：

| 模式 | 出题接口 | 实现状态 |
|------|---------|:--:|
| CATEGORY | `QuestionPort.searchByModule(key, count)` | ✅ |
| RANDOM | `QuestionPort.search("", moduleKeys, count)` | ✅ |
| REVIEW | `ProgressPort.findDueQuestions()` | 待 Phase 6 |

---

### Step 6 — submitAnswer + SSE

**最复杂的单步，编排 7 个阶段**：

```
1. sessionPort.findById → gateService.validateSession
2. questionPort.getById(questionId)
3. gateService.validateAnswer
4. scoreSlotPort.tryAcquire(30s)  ← Redis Semaphore，最多 10 并发
5. llmPort.score(question, answer) ← DeepSeek ~2-5s
6. gateService.validateScore → recordPort.save → sessionPort.update(currentIndex+1)
7. finally { scoreSlotPort.release() }
```

**SSE 推送时序**（总时长 ~2s，不含 LLM 等待）：

```
[0ms]    → event:score       {score:8}
[400ms]  → event:correct     {points:["提到了A","提到了B"]}
[800ms]  → event:missed      {points:["遗漏了C"]}
[1200ms] → event:suggestion  {text:"建议补充D..."}
[1600ms] → event:followUp    {question:"追问：请深入解释..."}
[2000ms] → event:done        {}
```

线程池：core=4, max=10, queue=50，超时 60s。异常时发 `error` 事件后 `completeWithError`。

---

### Step 7 — submitFollowUp

```
POST /recite/{sid}/followup  {recordId, followUpAnswer}
  └─ sessionPort.findById → gateService.validateSession + validateFollowUpDepth(<3)
  └─ recordPort.findById → questionPort.getById
  └─ llmPort.followUp(question, answer) → 反馈文字
  └─ recordPort.updateFollowUp(recordId, answer, feedback)
  └─ session.followUpDepth++ → sessionPort.update
```

**约束**：追问不评分、不进 user_progress、最多 3 层，每次递增 depth。

---

### Step 8 — finishRecite

```
POST /recite/{sid}/finish
  └─ sessionPort.findById → gateService.validateSession
  └─ recordPort.findBySessionId(userId, sid)
  └─ llmPort.generateReport(records)  ← Java 统计 + LLM 评语
  └─ session.status = "FINISHED" → sessionPort.update
```

**报告字段**：总分、平均分、题数、优势模块(≥7)、薄弱模块(≤4)、LLM 50 字评语。fire-and-forget 直接返回，不等 MQ。

---

## 三、依赖注入全图

```
ReciteController
  ├─ ReciteOrchestrationService
  │     ├─ QuestionPort          → PgVectorAdapter     (Phase 3)
  │     ├─ ReciteSessionPort     → ReciteSessionAdapter(Redis)
  │     ├─ ScoreSlotPort         → ScoreSlotAdapter    (Redis Semaphore)
  │     ├─ LlmPort               → DeepSeekLlmAdapter  (OkHttp)
  │     ├─ ReciteRecordPort      → ReciteRecordAdapter (PG)
  │     └─ ReciteGateService     (同包 @Service)
  ├─ ReciteSessionPort           → ReciteSessionAdapter
  ├─ ReciteRecordPort            → ReciteRecordAdapter
  └─ QuestionPort                → PgVectorAdapter
```

所有注入按类型自动装配，trigger 只依赖 domain SPI 接口（无 infra 依赖）。

---

## 四、偏离计划

| 计划 | 实际 | 原因 |
|------|------|------|
| finishRecite 发 RocketMQ 消息给 Phase 7/8 | 先跳过 MQ，直接返回报告 | RocketMQ 消费者未实现，Phase 7 再接入 |
| REVIEW 模式用 ProgressPort | 抛 UnsupportedOperationException | ProgressPort 在 Phase 6 |
| submitAnswer 返回 ScoreResultVO | 同计划 | — |
| getSession/getHistory 在 Step 9 | Step 5 顺带实现 | Controller 模板方便一次写完 |

---

## 五、编译结果

```
recite-types       SUCCESS
recite-api         SUCCESS
recite-domain      SUCCESS
recite-infrastructure SUCCESS
recite-trigger     SUCCESS
recite-app         SUCCESS
BUILD SUCCESS
```
