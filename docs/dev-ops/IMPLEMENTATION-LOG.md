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

---

## Phase 4 — auth 认证

**日期**：2026-06-17
**计划文档**：`PHASE-4-AUTH.md`

### 编码单元 #6：领域层基础

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `AuthException.java` | 14 | 继承 AppException，默认 401 |
| 2 | `UserEntity.java` | 32 | @Builder，10 个字段 |
| 3 | `AdminUserEntity.java` | 20 | @Builder，6 个字段 |
| 4 | `UserPort.java` | 15 | 3 个方法：findByPhone / findById / save |
| 5 | `AdminUserPort.java` | 11 | 1 个方法：findByUsername |

- **验收**：`mvn compile` 全部通过
- **commit**：`5104bfc`

### 编码单元 #7：API 层

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `LoginRequestDTO.java` | 12 | account + password |
| 2 | `RegisterRequestDTO.java` | 13 | phone + password + nickname |
| 3 | `LoginResultDTO.java` | 13 | token + role + nickname |
| 4 | `IAuthService.java` | 17 | 2 个端点：/auth/register + /auth/login |

- **验收**：`mvn compile` 通过
- **commit**：`26fa0f1`

### 编码单元 #8：持久层

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `UserDO.java` | 22 | @TableName("users")，11 列 |
| 2 | `UserMapper.java` | 10 | extends BaseMapper |
| 3 | `AdminUserDO.java` | 18 | @TableName("admin_users")，7 列 |
| 4 | `AdminUserMapper.java` | 10 | extends BaseMapper |

- **验收**：`mvn compile` 通过
- **commit**：`c8ec3f0`

### 编码单元 #9：适配器

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `UserPersistenceAdapter.java` | 55 | @Repository，DO↔Entity 转换，含防 null 默认值 |
| 2 | `AdminUserPersistenceAdapter.java` | 30 | @Repository，仅查询 |

- **验收**：`mvn compile` 通过
- **commit**：`94617d6`

### 编码单元 #10：配置 + 拦截器 + 控制器

| 步骤 | 文件 | 行数 | 说明 |
|:--:|------|:--:|------|
| 1 | `SaTokenConfig.java` | 14 | StpLogicJwtForSimple Bean |
| 2 | `UserContext.java` | 25 | TTL ThreadLocal，record Ctx 存 userId+role |
| 3 | `UserContextInterceptor.java` | 35 | OPTIONS 放行，token→查库→注入 TTL |
| 4 | `WebMvcConfig.java` | 18 | 注册拦截器 + 排除 /auth/**、/admin/auth/** 等 |
| 5 | `AuthController.java` | 70 | 注册（查重→哈希→入库→JWT）、登录（手机号/用户名→验密→JWT） |
| 6 | `AdminAuthController.java` | 35 | 管理员登录（用户名→验密→JWT+session role=ADMIN） |
| — | `PasswordUtils.java` | 25 | **补建**：SHA-256 哈希工具类（V2 types 中缺失） |
| — | `pom.xml`（parent） | — | 添加 `sa-token-jwt` 版本管理 |
| — | `pom.xml`（trigger） | — | 添加 `sa-token-jwt` 依赖 |

- **踩坑**：
  1. `sa-token-jwt` 包不在 `sa-token-spring-boot3-starter` 传递依赖中，需显式声明
  2. `PasswordUtils` 在旧 recite 项目中有，V2 recite-types 中缺失，编译时报"找不到符号"才补建
- **验收**：`mvn compile` 全部 6 个模块通过
- **commit**：`3802ca4`

### 小结

| 指标 | 计划 | 实际 |
|---|---|---|
| 总文件数 | ~20 | 22（含 2 pom + 1 PasswordUtils 补建） |
| 编码单元 | 5 | 5 |
| 编译错误 | 0 | 1（sa-token-jwt 缺依赖 + PasswordUtils 缺失） |
| Git commits | — | 5 |

**经验**：
1. Sa-Token JWT 模式需要单独引入 `sa-token-jwt` 依赖，starter 不包含
2. 从旧项目迁移代码时，需要检查 `recite-types` 中是否有遗漏的工具类
3. 拦截器中不要抛异常阻断请求——token 无效时放行，由后续 Sa-Token 注解拦截
