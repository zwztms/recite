# 实施日志

> 记录每个 Phase 的实际编码过程，供后续 Phase 参考。

---

## Phase 3 — knowledge 子域

**日期**：2026-06-17
**计划文档**：`PHASE-3-KNOWLEDGE.md`

### 编码单元 #1：领域层基础

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `KnowledgeException.java` | 14 | 继承 AppException，两个构造器 |
| 2 | `KnowledgeModuleEntity.java` | 32 | @Builder，9 个字段 |
| 3 | `QuestionEntity.java` | 30 | @Builder，含 float[] embedding |
| 4 | `EmbeddedQuestionVO.java` | 7 | record，不可变 |
| 5 | `ModulePort.java` | 28 | 6 个方法：CRUD + 上下线 + 计数增减 |
| 6 | `QuestionPort.java` | 28 | 9 个方法：搜索 + CRUD + 计数 |
| 7 | `EmbeddingPort.java` | 11 | 2 个方法：单条 + 批量 |
| 8 | `KnowledgeService.java` | 40 | importQuestions() — 批量 embedding→入库→计数 |

- **验收**：`mvn compile -pl recite-domain` 通过
- **commit**：`e4dc5cb`

### 编码单元 #2：API 层

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `KnowledgeModuleDTO.java` | 16 | 列表响应 |
| 2 | `ModuleCreateRequestDTO.java` | 13 | 创建请求 |
| 3 | `ModuleStatusRequestDTO.java` | 12 | 上下线请求 |
| 4 | `QuestionDTO.java` | 17 | 搜索响应 |
| 5 | `QuestionManageDTO.java` | 15 | 管理列表响应 |
| 6 | `QuestionEditDTO.java` | 10 | 编辑请求 |
| 7 | `ImportResultDTO.java` | 16 | 导入结果 |
| 8 | `IKnowledgeService.java` | 30 | 7 个端点，@RequestMapping + Spring MVC 注解 |

- **踩坑**：`recite-api/pom.xml` 缺少 `spring-boot-starter-web`，编译报"找不到 RequestMapping"。已修复。
- **验收**：`mvn compile -pl recite-types,recite-api` 通过
- **commit**：`2c25c26`

### 编码单元 #3：持久层

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `KnowledgeModuleDO.java` | 24 | @TableName + @TableId(AUTO) |
| 2 | `KnowledgeModuleMapper.java` | 10 | extends BaseMapper |
| 3 | `QuestionVectorDO.java` | 22 | 含 float[] embedding 字段 |
| 4 | `QuestionVectorMapper.java` | 35 | + 自定义 @Select（pgvector <=>运算符） |

- **踩坑**：`pom.xml` 中阿里云 Maven 仓库用 HTTP 被拦截，改为 HTTPS。RocketMQ `spring-boot-starter` 版本 5.2.0 不存在于 Maven Central，改为 `2.3.2`。
- **验收**：`mvn compile` 全部 4 个模块通过
- **commit**：`c4c7104`

### 编码单元 #4：适配器

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `ModuleManagementAdapter.java` | 100 | @Service，DO↔Entity 转换 |
| 2 | `PgVectorAdapter.java` | 95 | @Service，pgvector 搜索 + CRUD |
| 3 | `SiliconFlowEmbeddingAdapter.java` | 80 | @Service，OkHttp → SiliconFlow API |

- **验收**：`mvn compile` 全部通过
- **commit**：`9d202e6`

### 编码单元 #5：Controller

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `KnowledgeController.java` | 118 | @RestController，实现 IKnowledgeService |

- **备注**：`triggerImport()` 暂时返回占位结果，完整的文件导入（FileImportAdapter）后续实现。
- **验收**：`mvn compile` 全部模块通过
- **commit**：`77ab87d`

### 小结

| 指标 | 计划 | 实际 |
|---|---|---|
| 总文件数 | ~20 | 24（含 2 处 pom 修复） |
| 编码单元 | 5 | 5 |
| 编译错误 | 0 | 2（pom 缺依赖 + Maven 仓库配置） |
| Git commits | — | 5 |
| 用时 | — | ~30 min |

**经验**：
1. 每个编码单元结束后立即 `mvn compile`，避免错误累积
2. `recite-api` 作为纯接口模块容易被忽略 Spring Web 依赖
3. Maven 3.9+ 默认拦截 HTTP 仓库，需用 HTTPS 或配置 block 豁免
