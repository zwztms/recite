# Phase 1-6 缺陷修复报告

> 日期：2026-06-18 | 范围：3 项关键缺陷修复 | 编译：BUILD SUCCESS

---

## 一、修复清单

| # | 级别 | 位置 | 问题 | 修复方式 |
|:--:|:--:|------|------|------|
| 1 | 🔴 | 所有 Controller | API 零鉴权保护 | WebMvcConfig 加 SaInterceptor 路由鉴权 |
| 2 | 🔴 | `DeepSeekLlmAdapter:67` | `(Double)` 强转 Gson `LazilyParsedNumber` → ClassCastException | 改为 `(Number)` |
| 3 | 🟠 | `ReciteOrchestrationService:76` | REVIEW 模式抛 UnsupportedOperationException | 实现完整 REVIEW 流程 |

---

## 二、各修复详情

### 修复 1 — 零鉴权 → SaInterceptor 路由鉴权

**问题**：所有 `/recite/**` 和 `/admin/knowledge/**` 端点无需登录即可访问。`UserContext.getUserId()` 返回 `null` 时直接穿透到领域层，最终 NPE。

**改动文件**：`recite-trigger/.../config/WebMvcConfig.java`

**改动内容**：
- 新增 `SaInterceptor`（`StpUtil.checkLogin()`）拦截 `/recite/**`，未登录返回 401
- 新增 `SaInterceptor`（`StpUtil.checkRole("ADMIN")`）拦截 `/admin/knowledge/**`，非管理员返回 403
- 原有 `UserContextInterceptor` 不变，在鉴权之后执行，解析 token 注入用户上下文

```java
// WebMvcConfig.addInterceptors() — 新增两个 SaInterceptor
registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
        .addPathPatterns("/recite/**");

registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkRole("ADMIN")))
        .addPathPatterns("/admin/knowledge/**");
```

**白名单**（不鉴权）：`/auth/**`、`/admin/auth/**`、`/doc.html`、`/v3/**`、`/error`

---

### 修复 2 — DeepSeekLlmAdapter JSON 解析 ClassCastException

**问题**：Gson 的 `Map<String, Object>` 反序列化将 JSON 数字解析为 `LazilyParsedNumber`（extends `Number`），强制转为 `Double` 时抛 `ClassCastException`。

**触发条件**：每次 LLM 评分返回合法 JSON `{"score": 8, ...}` 时必现。

**改动文件**：`recite-infrastructure/.../adapter/llm/DeepSeekLlmAdapter.java`

**改动内容**：第 67 行，`(Double)` → `(Number)`：

```java
// Before
int score = ((Double) map.get("score")).intValue();
// After
int score = ((Number) map.get("score")).intValue();
```

---

### 修复 3 — REVIEW 模式完整实现

**问题**：Phase 6 已交付 `ProgressPort.findDueQuestions()`，但 Phase 5 的 `startRecite` 仍抛出 `UnsupportedOperationException`。

**改动文件**：`recite-domain/.../recite/service/ReciteOrchestrationService.java`

**改动内容**：

#### 3a. `startRecite` — REVIEW 分支

```
原有: case REVIEW -> throw new UnsupportedOperationException(...)
现为: case REVIEW -> {
    progressPort.findDueQuestions(userId, count)  // 取到期题目
    → questionPort.getById() 逐个查题目
    → 构建 EmbeddedQuestionVO(similarityScore = masteryScore/100)
    → 题目为空时抛 ReciteException("暂无到期复习题目")
}
```

#### 3b. `submitAnswer` — REVIEW 自评跳过 LLM

```
原有: 所有模式统一走 LLM 评分
现为: REVIEW → mapSelfAssessment(answer) 本地映射
      其他   → LLM 评分（原有逻辑不变）
```

自评映射：
| 输入 | 映射分值 |
|------|:--:|
| 包含"想起" | 9 |
| 包含"忘" | 2 |
| 默认（不确定） | 5 |

REVIEW 模式下不抢 Redis 信号量、不调 DeepSeek，直接返回 `ScoreResultVO`。

#### 3c. 新增 `mapSelfAssessment()` 辅助方法

```java
private int mapSelfAssessment(String answer) {
    if (answer == null) return 5;
    String a = answer.trim();
    if (a.contains("想起")) return 9;
    if (a.contains("忘")) return 2;
    return 5;
}
```

---

## 三、涉及文件

```
recite-v2/
├── recite-trigger/src/main/java/cn/bugstack/recite/trigger/config/
│   └── WebMvcConfig.java                          ← 修复 1（+2 个 SaInterceptor）
├── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/adapter/llm/
│   └── DeepSeekLlmAdapter.java                    ← 修复 2（1 行改动）
└── recite-domain/src/main/java/cn/bugstack/recite/domain/recite/service/
    └── ReciteOrchestrationService.java             ← 修复 3（startRecite + submitAnswer + mapSelfAssessment）
```

**总计 3 个文件，+35 行，-5 行**。

---

## 四、编译结果

```
recite-types          SUCCESS
recite-api            SUCCESS
recite-domain         SUCCESS
recite-infrastructure SUCCESS
recite-trigger        SUCCESS
recite-app            SUCCESS
BUILD SUCCESS
```

无新增依赖，无 API 变更，向后兼容。
