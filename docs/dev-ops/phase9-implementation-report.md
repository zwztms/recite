# Phase 9 编码报告 — 可观测性

> 日期：2026-06-18 | 范围：注解式链路追踪 + 全局异常处理 + Swagger + 运维后台 | 编译：BUILD SUCCESS

---

## 一、文件清单

### recite-types/annotation（2 个新建）

| 文件 | 职责 |
|------|------|
| `ReciteTraceRoot.java` | 方法级注解，标注入口方法。属性 `value()` 存入口方法名 |
| `ReciteTraceNode.java` | 方法级注解，标注内部环节。属性 `type()` (AUTH/CACHE/LLM/DB/BUSINESS/MQ/SSE)、`name()` (中文名) |

### recite-infrastructure/adapter/trace（7 个新建）

| 文件 | 职责 |
|------|------|
| `TraceContext.java` | ThreadLocal 存储当前请求 traceId，static 工具方法 |
| `TraceRunDO.java` | @TableName("trace_runs")，字段：id/traceId/userId/entryMethod/status/latencyMs/errorMsg/createdAt |
| `TraceRunMapper.java` | extends BaseMapper\<TraceRunDO\>，含 selectTodayStats / deleteBefore |
| `TraceNodeDO.java` | @TableName("trace_nodes")，字段：id/traceId/nodeName/nodeType/status/latencyMs/createdAt |
| `TraceNodeMapper.java` | extends BaseMapper\<TraceNodeDO\>，含 selectByTraceId / deleteBefore |
| `TraceAspect.java` | @Aspect 双切面：@ReciteTraceRoot（生成 traceId→写 RUNNING→proceed→写 SUCCESS/ERROR→finally clear）+ @ReciteTraceNode（无 trace 上下文时透传） |
| `TracePersistenceAdapter.java` | 封装 traceRunMapper/traceNodeMapper 的 insert/update 操作 |

### recite-infrastructure/adapter/trace（1 个新建 — 运维控制器）

| 文件 | 职责 |
|------|------|
| `AdminMonitorController.java` | @RestController + @SaCheckRole("ADMIN")，4 端点：listTraces 分页/traceDetail 详情/stats 今日统计/cleanTraces 清理 |

### recite-trigger/config（1 个新建 + 1 个修改）

| 文件 | 类型 | 职责 |
|------|:--:|------|
| `SpringDocConfig.java` | 新建 | @Configuration + OpenAPI Bean，标题"八股文背诵助手 API"，版本 2.0 |
| `WebMvcConfig.java` | 修改 | 追加 `/admin/monitor/**` 到 UserContext 白名单 + Sa-Token admin 鉴权 |

### recite-infrastructure/pom.xml（修改）

| 改动 | 原因 |
|------|------|
| 加 `spring-boot-starter-aop` | TraceAspect @Aspect 依赖 |
| 加 `spring-boot-starter-web` | AdminMonitorController @RestController 依赖 |
| 加 `recite-api` | Response 类引用 |
| 加 `sa-token-spring-boot3-starter` | @SaCheckRole 依赖 |

### DDL（修改）

| 文件 | 改动 |
|------|------|
| `docs/dev-ops/sql/schema.sql` | 追加 trace_runs + trace_nodes 建表 |

### 业务代码标注（16 个文件修改，22 处注解）

| Phase | 文件 | 注解 |
|:--:|------|------|
| 5 | `ReciteOrchestrationService.java` | @ReciteTraceRoot × 2（submitAnswer / finishRecite） |
| 5 | `ScoreSlotAdapter.java` | @ReciteTraceNode(CACHE, "获取评分槽") |
| 5 | `DeepSeekLlmAdapter.java` | @ReciteTraceNode × 2（LLM:DeepSeek评分 / LLM:LLM生成报告） |
| 5 | `ReciteRecordAdapter.java` | @ReciteTraceNode × 2（DB:保存背诵记录 / DB:查询背诵记录） |
| 5 | `ReciteSessionAdapter.java` | @ReciteTraceNode × 2（CACHE:读取背诵会话 / CACHE:更新会话缓存） |
| 6 | `ProgressAdapter.java` | @ReciteTraceNode × 2（DB:保存掌握度 / DB:更新掌握度） |
| 6 | `StreakAdapter.java` | @ReciteTraceNode × 3（DB:查询连续天数 / DB:新建连续天数 / DB:更新连续天数） |
| 7 | `ReportMessageAdapter.java` | @ReciteTraceNode(MQ, "发送报告消息") |
| 7 | `ReportGenerateConsumer.java` | @ReciteTraceRoot("reportGenerate") |
| 7 | `ReportPersistenceAdapter.java` | @ReciteTraceNode(DB, "保存学习档案") |
| 8 | `AchievementMessageAdapter.java` | @ReciteTraceNode(MQ, "发送成就消息") |
| 8 | `AchievementConsumer.java` | @ReciteTraceRoot("achievementEvaluate") |
| 8 | `AchievementPersistenceAdapter.java` | @ReciteTraceNode(DB, "写入徽章记录") |
| 8 | `NewBadgeRedisAdapter.java` | @ReciteTraceNode(CACHE, "标记新徽章") |

---

## 二、功能 1：注解式链路追踪

### 实现链路

```
@ReciteTraceRoot
  ├─ TraceAspect.traceRoot()
  │   ├─ UUID 前 8 位 → traceId
  │   ├─ TraceContext.setTraceId(traceId)
  │   ├─ INSERT trace_runs (status=RUNNING)
  │   ├─ proceed() → 业务方法链
  │   │
  │   │   @ReciteTraceNode (每个被拦截):
  │   │   ├─ TraceAspect.traceNode()
  │   │   │   ├─ traceId = TraceContext.getTraceId()
  │   │   │   ├─ if null → proceed() 透传（不在 trace 上下文中）
  │   │   │   ├─ INSERT trace_nodes (status=RUNNING)
  │   │   │   ├─ proceed()
  │   │   │   └─ UPDATE trace_nodes (status=SUCCESS/ERROR, latencyMs)
  │   │
  │   ├─ UPDATE trace_runs (status=SUCCESS, latencyMs)
  │   └─ finally: TraceContext.clear()

异常路径:
  try { ... } catch (Throwable e) {
      UPDATE trace_runs/trace_nodes (status=ERROR, errorMsg)
      throw e;  ← 不吞异常，继续往上抛
  }
```

### 四条 trace 链路

| 链路 | 触发 | 节点数 |
|------|------|:--:|
| submitAnswer | POST /recite/{sid}/submit | 6 (CACHE→LLM→DB×2→CACHE) |
| finishRecite | POST /recite/{sid}/finish | 9 (CACHE→DB→MQ×2→DB×3→CACHE) |
| reportGenerate | MQ recite-report-topic | 3 (DB→LLM→DB) |
| achievementEvaluate | MQ recite-achievement-topic | 3 (DB→CACHE→DB) |

---

## 三、功能 2：全局异常处理

已存在于 bugfix 阶段（Phase 1-6 缺陷修复），Phase 9 未重复创建。

```java
@RestControllerAdvice
GlobalExceptionHandler
  ├─ @ExceptionHandler(AppException.class) → return Response.fail(code, msg)
  └─ @ExceptionHandler(Exception.class)    → return Response.fail("500", "服务器内部错误")
```

---

## 四、功能 3：Swagger API 文档

- 依赖：`springdoc-openapi-starter-webmvc-ui:2.7.0`（已在父 POM）
- 配置：`SpringDocConfig.java`（OpenAPI Bean）
- 访问：`http://localhost:18081/doc.html`
- 自动扫描所有 Controller，无需手动注册端点

---

## 五、功能 4：运维后台 API

| 端点 | 方法 | 用途 |
|------|:--:|------|
| `/admin/monitor/traces?page=1&size=20` | GET | 分页查 trace_runs |
| `/admin/monitor/traces/{traceId}` | GET | trace 详情 + 节点列表 |
| `/admin/monitor/stats` | GET | 今日统计（总数/均耗时/异常数） |
| `/admin/monitor/traces?before=30` | DELETE | 清理 N 天前链路数据 |

鉴权：Sa-Token `@SaCheckRole("ADMIN")`，配合 WebMvcConfig 拦截器。

---

## 六、架构决策

| 决策 | 原因 |
|------|------|
| 运维控制器放 infrastructure | 链路追踪是纯基础设施，不涉及业务域；trigger 模块无法直接引用 infra 的 Mapper |
| @ReciteTraceRoot 放编排层而非 Controller | submitAnswer 通过 SSE 线程池异步执行，放 Controller 层会导致 traceId 在异步线程丢失 |
| TraceContext 用普通 ThreadLocal | 当前所有 trace 在单线程内完成（编排层注解在异步线程内，节点注解同线程），无需 TTL |
| 不做域层抽象 | 链路追踪不是业务子域，不创建 domain/monitor 包 |
| 统计查询不加注解 | buildStats() 内的 7 个轻量 SQL 查询一一标注过于噪音 |
| DDL 追加到已有 schema.sql | 保持单文件管理，避免 SQL 碎片化 |

---

## 七、新增数据库表

```sql
trace_runs  (id, trace_id, user_id, entry_method, status, latency_ms, error_msg, created_at)
trace_nodes (id, trace_id, node_name, node_type, status, latency_ms, created_at)
```

两表通过 `trace_id` 关联，30 天自动清理（运维 API）。

---

## 八、编译结果

```
recite-types          SUCCESS
recite-api            SUCCESS
recite-domain         SUCCESS
recite-infrastructure SUCCESS
recite-trigger        SUCCESS
recite-app            SUCCESS
BUILD SUCCESS
```

---

## 九、提交记录

| Commit | 说明 |
|------|------|
| `e4aa505` | phase9-1: 注解定义 — @ReciteTraceRoot + @ReciteTraceNode + AOP依赖 |
| `4128bba` | phase9-2: TraceContext + trace_runs/trace_nodes DO与Mapper |
| `592b6f4` | phase9-3: TraceAspect双切面 + TracePersistenceAdapter — AOP核心 |
| `212658e` | phase9-4: SpringDocConfig Swagger配置 + WebMvcConfig追加/admin/monitor白名单 |
| `70a300b` | phase9-5: AdminMonitorController运维API — 4端点 + Mapper查询方法 + Sa-Token鉴权 |
| `8155be1` | phase9-6: 标注submitAnswer链路 — @ReciteTraceRoot + 6个@ReciteTraceNode |
| `2e71865` | phase9-7: 标注finishRecite链路 — @ReciteTraceRoot + 8个@ReciteTraceNode |
| `72c95b2` | phase9-8: 标注MQ消费者 — reportGenerate + achievementEvaluate + 4内部节点 |
| `466c6ab` | phase9-9: DDL — trace_runs + trace_nodes 建表脚本 |
