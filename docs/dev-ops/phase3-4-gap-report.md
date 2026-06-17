# Phase 3/4 Gap 修复编码报告

> 日期：2026-06-17 | 提交：f6da2ad | 审查范围：recite-v2 Phase 3 knowledge + Phase 4 auth

---

## Gap 1：PgVectorAdapter 向量搜索未接入

### 解决问题

`PgVectorAdapter.search()` 没有真正执行向量语义搜索，用全零占位向量 + `selectList(null)` 降级，导致背诵出题时返回的是数据库顺序列表而非语义最相关的题目。

### 出现原因

Phase 3 编码时采用渐进策略，先搭 CRUD 骨架，向量搜索部分预留了 `EmbeddingPort` 注入位但未接入，计划"后续再补"。Mapper 层的 `searchByVector` SQL 已写好（含 `<=>` 余弦距离 + HNSW），适配器层没有调用。

### 处理方法

1. `PgVectorAdapter` 注入 `EmbeddingPort`，`search()` 中先调 `embeddingPort.embed(query)` 获取 `float[1024]`
2. 将向量按 pgvector 格式序列化为 `[0.1,0.2,...]` 字符串
3. 有模块过滤时调 `mapper.searchByVector()`，无模块过滤时调新增的 `mapper.searchByVectorAll()`
4. `QuestionVectorDO` 加 `@TableField(exist=false) similarity` 字段接收 SQL 计算列
5. `QuestionVectorMapper` 新增 `searchByVectorAll` 方法（不限制模块的向量搜索）
6. `searchByModule()` 增加 null 保护

**改动文件**：`PgVectorAdapter.java`、`QuestionVectorDO.java`、`QuestionVectorMapper.java`

---

## Gap 2：文件导入功能未实现

### 解决问题

`KnowledgeController.triggerImport()` 返回假数据 `"导入功能待实现"`，管理员无法批量导入题目。文件解析、embedding 调用、向量入库、文件归档全链路缺失。

### 出现原因

Phase 3 编码时聚焦于模块管理和题目 CRUD，文件导入的 I/O 逻辑被标记为"后续实现"。计划文档设计了完整流程（扫描→解析→embedding→入库→归档），但没有落到代码。

### 处理方法

1. 新建 SPI 接口 `FileImportPort`（domain/knowledge/port/out/），定义 `doImport()` 契约
2. 新建值对象 `ImportResultVO`（domain），避免 SPI 依赖 api 模块 DTO
3. 新建 `FileImportAdapter`（infra/adapter/imports/），实现完整链路：
   - 扫描 `docs/import/` 下的 `.json` / `.md` 文件
   - JSON 解析：Gson 反序列化为 `List<QuestionEntity>`
   - Markdown 解析：按 `## ` 分段，首行作标题、余作答案
   - 自动注册缺失模块 → 调 `KnowledgeService.importQuestions()` → 文件移入 backup/
4. `KnowledgeController` 注入 `FileImportPort`（接口，非实现类），保持 trigger 不依赖 infra
5. `KnowledgeService` 加 `@Service` 注册为 Spring Bean

**改动文件**：新增 `FileImportPort.java`、`ImportResultVO.java`、`FileImportAdapter.java`；修改 `KnowledgeController.java`、`KnowledgeService.java`

---

## Gap 3：用户名登录降级为手机号查询

### 解决问题

`AuthController.login()` 非手机号格式时用了 `findByPhone(account)` 查用户，导致用昵称登录永远找不到用户（昵称不是手机号格式）。

### 出现原因

编码时 `UserPort` 只定义了 `findByPhone` 和 `findById`，Controller 的 else 分支偷懒复用了 `findByPhone`。计划文档明确写了"用户名登录 → UserPort.findByNickname"。

### 处理方法

1. `UserPort` 新增 `findByNickname(String nickname)` 方法
2. `UserPersistenceAdapter` 实现：`LambdaQueryWrapper.eq(UserDO::getNickname, nickname)`
3. `AuthController.login()` 的 else 分支改为 `userPort.findByNickname(account)`

**改动文件**：`UserPort.java`、`UserPersistenceAdapter.java`、`AuthController.java`

---

## Gap 4：全局异常处理器缺失

### 解决问题

`AuthException`(code=401) 和 `KnowledgeException`(code=400) 抛出后没有 `@ControllerAdvice` 统一拦截，Spring 默认返回 500 或空响应体，前端收到的不是标准 `Response<T>` 格式。

### 出现原因

计划文档五（Phase 3）和七（Phase 4）的文件清单中都没有列出异常处理器。`AuthException` 和 `KnowledgeException` 已正确继承 `AppException`，但缺少全局拦截层。

### 处理方法

新建 `GlobalExceptionHandler`（trigger/config/）：
- `@ExceptionHandler(AppException.class)` → `Response.fail(e.getCode(), e.getMessage())`
- `@ExceptionHandler(Exception.class)` → `Response.fail("500", "服务器内部错误")`
- 所有 `@RestControllerAdvice` 下的异常统一为 HTTP 200 + 业务 code

之后 Phase 5+ 新增的任何 `XxxException` 只需继承 `AppException` 即可自动被拦截。

**改动文件**：新增 `GlobalExceptionHandler.java`

---

## Gap 5：Sa-Token JWT 超时配置为 30 天

### 解决问题

`application-dev.yml` 中 `sa-token.timeout: 2592000`（30 天），与计划要求的 7 天（604800 秒）不一致。JWT 无状态，签发后无法撤销，超时过长增加安全风险。

### 出现原因

配置项手误，30 天的值是从 Sa-Token 默认值或 V1 项目复制时带入的。

### 处理方法

`application-dev.yml` 第 30 行 `timeout: 2592000` → `timeout: 604800`

**改动文件**：`application-dev.yml`

---

## 汇总

| Gap | 解决 | 原因 | 方法 | 文件数 |
|:--:|------|------|------|:--:|
| 1 | 向量搜索接入 | 渐进编码未收尾 | 注入 EmbeddingPort → pgvector <=> | 3 改 |
| 2 | 文件导入上线 | 标记 TODO 未实现 | FileImportPort(SPI) + FileImportAdapter | 3 新 + 2 改 |
| 3 | 用户名登录修正 | else 分支偷懒 | UserPort + findByNickname | 3 改 |
| 4 | 全局异常拦截 | 计划文件清单漏项 | GlobalExceptionHandler | 1 新 |
| 5 | JWT 超时修正 | 配置手误 | 2592000 → 604800 | 1 改 |

**总计**：4 个新文件 + 9 个修改文件，BUILD SUCCESS，零编译错误。
