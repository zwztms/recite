# Phase 11 — 测试 实施计划

> 日期：2026-06-16 | 状态：计划已审批，待编码

---

## 一、测试策略

三层测试金字塔，按 P0 → P1 → P2 优先级实施。

| 层级 | 技术 | 范围 | 优先级 |
|:--:|------|------|:--:|
| 单元测试 | JUnit 5 + AssertJ | 领域服务（纯函数） | P0 |
| 集成测试 | SpringBootTest + MockMvc | Controller + Service + DB | P1 |
| E2E 测试 | TestRestTemplate | 完整 HTTP 链路 | P2 |

报告框架：Allure（`mvn test` → `allure serve` 自动生成 HTML）

---

## 二、P0 — 单元测试（核心算法）

**不启动 Spring，纯 JUnit。** 测试领域层的纯函数逻辑。

| # | 测试类 | 被测类 | 测试点 |
|:--:|------|------|------|
| 1 | `SpacedRepetitionServiceTest` | `SpacedRepetitionService` | 首次背诵初始值、高分拉大间隔、低分重置1天、ease边界、mastery加权、自评三档、连续多次间隔变化 |
| 2 | `StreakServiceTest` | `StreakService` | 首次签到、连续两天、同日重复不计、断签重置、断签后恢复、最长记录保持 |
| 3 | `ReciteGateServiceTest` | `ReciteGateService` | 答案空/null/超长、分数0/5/10/11边界、追问深度0/3/4边界、会话userId不匹配 |
| 4 | `BadgeRegistryTest` | `BadgeRegistry` + `AchievementService` | 背诵量5枚条件、质量5枚、坚持5枚、组合4枚、隐藏8枚、已获得不重复发、一次会话获得多枚 |
| 5 | `KnowledgeServiceTest` | `KnowledgeService` | JSON解析、Markdown分段、moduleKey推导、空文件处理 |
| 6 | `ReportServiceTest` | `ReportService` | 聚合统计、优势/薄弱判断(≥7/≤4)、空记录、单题记录 |

**AssertJ 风格示例**：
```java
@Test
void highScoreShouldIncreaseInterval() {
    var current = createProgress(interval=1, ease=2.5, mastery=50);
    var result = service.calculateAfterScore(current, 9); // 高分
    assertThat(result.getReviewInterval()).isGreaterThan(1);
    assertThat(result.getEaseFactor()).isGreaterThan(2.5);
    assertThat(result.getMasteryScore()).isGreaterThan(50);
}

@Test
void lowScoreShouldResetInterval() {
    var current = createProgress(interval=14, ease=3.0, mastery=80);
    var result = service.calculateAfterScore(current, 3); // 低分
    assertThat(result.getReviewInterval()).isEqualTo(1); // 重置为1天
    assertThat(result.getEaseFactor()).isLessThan(3.0);
}
```

---

## 三、P1 — 集成测试（关键 API）

**启动 Spring 上下文 + 测试数据库。** MockMvc 模拟 HTTP 请求。

| # | 测试类 | 测试场景 | 断言 |
|:--:|------|------|------|
| 1 | `AuthControllerIT` | 注册→登录→token有效→访问受保护端点 | 200, token非空, role正确 |
| 2 | `KnowledgeControllerIT` | 创建模块→列表→上下线→删除→导入题目 | 模块数变化, 题目入库 |
| 3 | `ReciteControllerIT` | 开始背诵→提交答案→SSE流式评分→追问→结束→查看历史 | sessionId非空, SSE有6段, 报告有分数 |
| 4 | `ReportControllerIT` | 仪表盘查询→档案列表→轮询报告 | 统计数 ≥0, 档案分页正确 |
| 5 | `AchievementControllerIT` | 徽章墙→新徽章轮询→确认已读 | 46枚返回, 轮询返回数组 |

**MockMvc 示例**：
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class ReciteControllerIT {

    @Autowired MockMvc mvc;

    @Test
    void fullReciteFlow() throws Exception {
        // 1. 登录
        var loginRes = mvc.perform(post("/auth/login")
            .content("""
                {"account":"15386747351","password":"zw123456"}""")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        String token = JsonPath.read(loginRes.getResponse().getContentAsString(), "$.data.token");

        // 2. 开始背诵
        var startRes = mvc.perform(post("/recite/start")
            .header("Authorization", token)
            .content("""{"mode":"CATEGORY","moduleKeys":["java-basics"],"count":3}""")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
            .andReturn();

        // 3. 提交答案（SSE）
        // ... 解析 SseEmitter 返回
    }
}
```

---

## 四、P2 — E2E 测试

使用 TestRestTemplate 走完整 HTTP 链路，最接近真实用户行为。

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ReciteE2ETest {

    @Autowired TestRestTemplate rest;

    @Test
    void completeReciteSession() {
        // 登录 → 开始 → 答3题 → 追问1次 → 结束 → 查报告 → 查徽章
    }
}
```

---

## 五、测试数据准备

使用 `@Sql` 注解加载测试数据，避免依赖生产数据：

```sql
-- src/test/resources/test-data.sql
INSERT INTO users (id, phone, password_hash, nickname, role, status)
VALUES (1, '13800138000', 'xxx', '测试用户', 'USER', 'ACTIVE');

INSERT INTO knowledge_modules (module_key, module_name, status, sort_order)
VALUES ('java-basics', 'Java基础', 'ONLINE', 1);

INSERT INTO question_vectors (id, question, content, module_key, embedding)
VALUES ('q1', '什么是面向对象？', '...', 'java-basics', array_fill(0::real, ARRAY[1024]));
```

---

## 六、Allure 报告

pom.xml 已有 `allure-junit5` 依赖。测试类加 `@Feature("背诵核心")` `@Story("评分")` 注解 → Allure 自动生成分类报告。

```bash
mvn clean test
allure serve target/allure-results   # 浏览器打开 HTML 报告
```

---

## 七、文件清单

```
recite-v2/
│
└── recite-domain/src/test/java/cn/bugstack/recite/domain/
    ├── progress/
    │   ├── SpacedRepetitionServiceTest.java        P0
    │   └── StreakServiceTest.java                  P0
    ├── recite/
    │   └── ReciteGateServiceTest.java              P0
    ├── achievement/
    │   └── BadgeRegistryTest.java                  P0
    ├── knowledge/
    │   └── KnowledgeServiceTest.java               P0
    └── report/
        └── ReportServiceTest.java                  P0

└── recite-app/src/test/java/cn/bugstack/recite/
    ├── AuthControllerIT.java                       P1
    ├── KnowledgeControllerIT.java                  P1
    ├── ReciteControllerIT.java                     P1
    ├── ReportControllerIT.java                     P1
    ├── AchievementControllerIT.java                P1
    ├── ReciteE2ETest.java                          P2
    └── resources/
        └── test-data.sql                           测试数据
```

**总计 ~13 个测试类**（6 单元 + 5 集成 + 1 E2E + SQL）

---

## 八、测试执行顺序

| 步骤 | 测试 | 对应 Phase |
|:--:|------|:--:|
| 1 | `SpacedRepetitionServiceTest` + `StreakServiceTest` | Phase 6 |
| 2 | `BadgeRegistryTest` | Phase 8 |
| 3 | `ReciteGateServiceTest` | Phase 5 |
| 4 | `KnowledgeServiceTest` | Phase 3 |
| 5 | `ReportServiceTest` | Phase 7 |
| 6 | `AuthControllerIT` | Phase 4 |
| 7 | `KnowledgeControllerIT` | Phase 3 |
| 8 | `ReciteControllerIT` | Phase 5 |
| 9 | `ReportControllerIT` | Phase 7 |
| 10 | `AchievementControllerIT` | Phase 8 |
| 11 | `ReciteE2ETest` | Phase 3-8 全部 |

---

## 九、编码建议

测试不是等 Phase 11 才写——**每完成一个 Phase 就写对应的测试**（至少 P0 单元测试）。这样可以在开发过程中持续验证，而不是最后补。
