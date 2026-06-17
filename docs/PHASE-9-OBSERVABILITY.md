# Phase 9 — 可观测性 实施计划

> 日期：2026-06-16 | 状态：计划已审批，待编码

---

## 一、定位

不是子域，是**基础设施横切关注点**。包含四块：

| 模块 | 说明 |
|---|---|
| 注解式链路追踪 | `@ReciteTraceRoot` / `@ReciteTraceNode` → AOP → `trace_runs` + `trace_nodes` |
| 全局异常处理 | `@RestControllerAdvice` 统一拦截 → 标准 Response |
| Swagger 文档 | SpringDoc 自动扫描 → `/doc.html` |
| 运维后台 API | 4 个端点，为 AdminMonitor.vue（Phase 10 前端）提供数据 |

---

## 二、要实现哪些功能，怎么实现

### 功能 1：注解式链路追踪

**做什么**：在关键方法上加注解，AOP 自动记录入口方法、环节名称、节点类型、耗时、状态、异常信息。

**完整实现链路**：

```
请求进入 Controller
  │
  ▼
@ReciteTraceRoot("submitAnswer")     ← 标注在 Controller 方法上
  │
  ├─ AOP @Around Before:
  │   ① 生成 traceId = UUID 前 8 位，如 "a3f8b2c1"
  │   ② TraceContext.setTraceId(traceId)    ← 存入 ThreadLocal
  │   ③ INSERT INTO trace_runs (trace_id, user_id, entry_method, status='RUNNING')
  │   ④ 记录 startTime = System.currentTimeMillis()
  │   ⑤ proceed() → 执行 Controller 方法
  │
  ├─ 业务方法链（每个 @ReciteTraceNode 被拦截）:
  │
  │   @ReciteTraceNode(type="AUTH", name="校验会话")
  │     [Around Before]  INSERT trace_nodes (trace_id, node_name, node_type,
  │                                          status='RUNNING')
  │                      记录 nodeStartTime
  │     [proceed]         执行业务逻辑
  │     [Around After]    UPDATE trace_nodes SET status='SUCCESS',
  │                                          latency_ms=now-nodeStartTime
  │
  │   @ReciteTraceNode(type="CACHE", name="获取评分槽")
  │     ... 同上 ...
  │
  │   @ReciteTraceNode(type="LLM", name="DeepSeek评分")    ← 耗时最长(~3s)
  │     ... 同上 ...
  │
  │   @ReciteTraceNode(type="DB", name="保存背诵记录")
  │     ... 同上 ...
  │
  │   @ReciteTraceNode(type="DB", name="更新掌握度")
  │     ... 同上 ...
  │
  │   @ReciteTraceNode(type="BUSINESS", name="间隔计算")
  │     ... 同上 ...
  │
  │   @ReciteTraceNode(type="SSE", name="流式推送结果")
  │     ... 同上 ...
  │
  ├─ AOP @Around AfterReturning:
  │   UPDATE trace_runs SET status='SUCCESS', latency_ms=总耗时
  │
  └─ AOP @Around AfterThrowing:
      UPDATE trace_runs SET status='ERROR', error_msg=e.getMessage()
      (不吞异常，继续往上抛给 GlobalExceptionHandler)

  最终 TraceContext.clear()  ← 防止内存泄漏
```

**标注目标方法（各 Phase 已有代码上加注解，零侵入）**：

```
submitAnswer 链路 (9 节点):
  AUTH → CACHE(获取槽) → VALIDATE → LLM(评分) → DB(存记录) →
  DB(更新进度) → BUSINESS(间隔) → CACHE(更新会话) → SSE(推送)

finishRecite 链路 (8 节点):
  AUTH → CACHE(读会话) → DB(查记录) → DB(查进度) →
  BUSINESS(统计) → MQ(发报告) → MQ(发成就) → CACHE(清理会话)

MQ 消费者独立 trace:
  ReportGenerateConsumer   RUNNING → DB(查记录) → LLM(生成) → DB(存档案) → SUCCESS
  AchievementConsumer      RUNNING → DB(统计) → BUSINESS(评估) → DB(写徽章) → SUCCESS
```

---

### 功能 2：全局异常处理

**做什么**：所有 Controller 抛出的异常统一拦截，返回标准 Response，不暴露内部细节。

**实现链路**：

```
业务代码任意位置:
  throw new KnowledgeException("模块不存在")
  throw new ReciteException("会话已过期")
  throw new AppException("500", "未知错误")
  throw new RuntimeException("NPE")
       │
       ▼  (不 try-catch，往上抛)
       │
@RestControllerAdvice
GlobalExceptionHandler
  │
  ├─ @ExceptionHandler(AppException.class)
  │   → log.warn("业务异常: {} - {}", e.getCode(), e.getMessage())
  │   → return Response.fail(e.getCode(), e.getMessage())
  │
  └─ @ExceptionHandler(Exception.class)
      → log.error("未知异常", e)
      → return Response.fail("500", "服务器内部错误")
      (不返回 e.getMessage()，防止泄露)
```

**已有的异常类**：
```java
AppException (types)  ← 基类
  ├─ KnowledgeException (Phase 3)
  ├─ AuthException (Phase 4)
  ├─ ReciteException (Phase 5)
  ├─ ProgressException (Phase 6)
  ├─ ReportException (Phase 7)
  └─ AchievementException (Phase 8)
```

---

### 功能 3：Swagger API 文档

- SpringDoc 已在 pom.xml 依赖中（`springdoc-openapi-starter-webmvc-ui`）
- 只需一个配置类：
  ```java
  @Configuration
  public class SpringDocConfig {
      @Bean
      OpenAPI reciteOpenAPI() {
          return new OpenAPI()
              .info(new Info()
                  .title("八股文背诵助手 API")
                  .version("2.0")
                  .description("recite-v2 DDD 六模块"));
      }
  }
  ```
- 访问 `http://localhost:18081/doc.html` → 自动列出所有 Controller 端点

---

### 功能 4：运维后台 API

为 AdminMonitor.vue（Phase 10）提供数据：

```
GET /admin/monitor/traces?page=1&size=20
  AdminMonitorController.listTraces(page, size)
    └→ SELECT * FROM trace_runs
        ORDER BY created_at DESC LIMIT ? OFFSET ?
        → [{traceId, entryMethod, status, latencyMs, createdAt}]

GET /admin/monitor/traces/{traceId}
  AdminMonitorController.traceDetail(traceId)
    ├─ SELECT * FROM trace_runs WHERE trace_id=?
    └─ SELECT * FROM trace_nodes WHERE trace_id=? ORDER BY id ASC
        → {trace: {...}, nodes: [{nodeName, nodeType, status, latencyMs}, ...]}

GET /admin/monitor/stats
  AdminMonitorController.stats()
    └→ 一条 SQL 聚合:
        SELECT
          COUNT(*) FILTER(WHERE created_at > today) AS todayTotal,
          AVG(latency_ms) FILTER(WHERE created_at > today) AS avgLatency,
          COUNT(*) FILTER(WHERE status='ERROR' AND created_at > today) AS todayErrors
        FROM trace_runs
        → {todayTotal, avgLatency, todayErrors}

DELETE /admin/monitor/traces?before=30
  AdminMonitorController.cleanTraces(days)
    ├─ DELETE FROM trace_nodes WHERE created_at < NOW() - INTERVAL '? days'
    └─ DELETE FROM trace_runs WHERE created_at < NOW() - INTERVAL '? days'
        → {deletedRuns: N, deletedNodes: M}
```

---

## 三、涉及存储

| 存储 | 表 | 用途 |
|---|---|---|
| PostgreSQL | `trace_runs` | 每次请求根记录：traceId, 方法, 状态, 耗时, 异常 |
| PostgreSQL | `trace_nodes` | 每个环节明细：节点名, 类型, 耗时, 状态 |
| JVM 内存 | `TraceContext` (ThreadLocal) | 同一请求内传递 traceId |

---

## 四、类清单与关系

```
┌──────────────────────────────────────────────────────────────────┐
│                     recite-types                                  │
│                                                                   │
│  annotation/                                                      │
│  ├─ @ReciteTraceRoot    方法级，标注入口方法                       │
│  └─ @ReciteTraceNode    方法级，标注内部环节                       │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│                   recite-trigger                                  │
│                                                                   │
│  config/                                                          │
│  ├─ GlobalExceptionHandler   @RestControllerAdvice               │
│  ├─ SpringDocConfig          Swagger /doc.html                    │
│  └─ WebMvcConfig             补充 /admin/monitor/** 白名单        │
│                                                                   │
│  http/                                                            │
│  └─ AdminMonitorController   4 个运维 API                         │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│             recite-infrastructure/adapter/trace/                  │
│                                                                   │
│  TraceAspect               @Aspect + @Around，核心 AOP            │
│  TraceContext              ThreadLocal，存当前请求 traceId         │
│  TracePersistenceAdapter   写 trace_runs / trace_nodes            │
│  TraceRunDO + TraceRunMapper           trace_runs 持久层          │
│  TraceNodeDO + TraceNodeMapper         trace_nodes 持久层         │
└──────────────────────────────────────────────────────────────────┘
```

无 domain 层 —— 链路追踪是纯基础设施，不涉及业务领域。

---

## 五、每个类/接口的详细职责

### 5.1 注解（recite-types）

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReciteTraceRoot {
    String value();  // 入口方法名，如 "submitAnswer"
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReciteTraceNode {
    String type();   // AUTH / CACHE / VALIDATE / LLM / DB / BUSINESS / MQ / SSE
    String name();   // 中文名称，如 "DeepSeek评分"
}
```

### 5.2 AOP 切面

```java
@Aspect
@Component
public class TraceAspect {

    // @Around("@annotation(cn.bugstack.recite.types.annotation.ReciteTraceRoot)")
    // 生成 traceId → INSERT trace_runs → proceed → UPDATE status

    // @Around("@annotation(cn.bugstack.recite.types.annotation.ReciteTraceNode)")
    // 获取 traceId (ThreadLocal) → INSERT trace_nodes → proceed → UPDATE latency
}
```

### 5.3 ThreadLocal 上下文

```java
public final class TraceContext {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    public static void setTraceId(String traceId) { TRACE_ID.set(traceId); }
    public static String getTraceId() { return TRACE_ID.get(); }
    public static void clear() { TRACE_ID.remove(); }
}
```

### 5.4 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public Response<?> handleAppException(AppException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Response.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Response<?> handleUnknown(Exception e) {
        log.error("未知异常", e);
        return Response.fail("500", "服务器内部错误");
    }
}
```

### 5.5 运维控制器

```java
@RestController
@RequestMapping("/admin/monitor")
public class AdminMonitorController {

    @GetMapping("/traces")
    Response<PageResult<TraceRunVO>> listTraces(
        @RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="20") int size);

    @GetMapping("/traces/{traceId}")
    Response<TraceDetailVO> traceDetail(@PathVariable String traceId);

    @GetMapping("/stats")
    Response<TraceStatsVO> stats();

    @DeleteMapping("/traces")
    Response<Map<String,Integer>> cleanTraces(@RequestParam(defaultValue="30") int before);
}
```

---

## 六、文件清单

```
recite-v2/
│
├── recite-types/src/main/java/cn/bugstack/recite/types/
│   └── annotation/
│       ├── ReciteTraceRoot.java
│       └── ReciteTraceNode.java
│
├── recite-trigger/src/main/java/cn/bugstack/recite/trigger/
│   ├── config/
│   │   ├── GlobalExceptionHandler.java
│   │   └── SpringDocConfig.java
│   └── http/
│       └── AdminMonitorController.java
│
└── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/
    └── adapter/trace/
        ├── TraceAspect.java
        ├── TraceContext.java
        ├── TracePersistenceAdapter.java
        ├── TraceRunDO.java
        ├── TraceRunMapper.java
        ├── TraceNodeDO.java
        └── TraceNodeMapper.java
```

**总计 ~12 个文件**（2 types + 3 trigger + 7 infra）。无 domain 层，无 api 层。

---

## 七、编码顺序

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `@ReciteTraceRoot` `@ReciteTraceNode` | 注解定义，其他类依赖 |
| 2 | `TraceContext.java` | ThreadLocal |
| 3 | `TraceRunDO` `TraceNodeDO` + Mapper | 持久层 |
| 4 | `TraceAspect.java` `TracePersistenceAdapter.java` | 核心 AOP |
| 5 | `GlobalExceptionHandler.java` | 异常处理 |
| 6 | `SpringDocConfig.java` | Swagger |
| 7 | `AdminMonitorController.java` | 运维 API |

每步完成后 `mvn compile` 验证。
