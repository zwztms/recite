# Phase 3 — knowledge 子域 实施计划

> 日期：2026-06-17 | 状态：计划已审批，待编码

---

## 一、子域职责

知识库子域（支撑域）负责三个能力：

| 能力 | 说明 |
|---|---|
| 模块管理 | knowledge_modules 表的 CRUD，模块上下线 |
| 题目搜索 | pgvector 语义搜索，按模块/关键词检索题目 |
| 文件导入 | 扫描 `docs/import/`，解析 JSON/Markdown，embedding 后入库 |

此子域不依赖其他子域，只依赖 `recite-types`。后续 recite 子域（Phase 5）通过 `QuestionPort` 获取题目。

---

## 二、要实现哪些功能，怎么实现

### 功能 1：列出全部模块

**做什么**：管理员查看所有模块（含状态、题目数），按 sortOrder 排序。

**完整调用链路**：

```
GET /admin/knowledge/modules

KnowledgeController.listModules()
  │
  ▼
ModulePort.listAll()
  │
  └→ SQL: SELECT * FROM knowledge_modules ORDER BY sort_order ASC

  返回 List<KnowledgeModuleEntity>
  │
  ▼
Controller 层 Entity → DTO 转换:
  KnowledgeModuleEntity → KnowledgeModuleDTO
  (id, moduleKey, moduleName, description, status, sortOrder, questionCount)

  └→ Response.ok(List<KnowledgeModuleDTO>)
```

---

### 功能 2：创建模块

**做什么**：管理员新建一个知识模块。

**完整调用链路**：

```
POST /admin/knowledge/modules  {moduleKey, moduleName, description, sortOrder}

KnowledgeController.createModule(dto)
  │
  ├─① 校验 moduleKey 唯一性
  │   ModulePort.findByKey(dto.moduleKey)
  │   └→ SELECT * FROM knowledge_modules WHERE module_key=?
  │   已存在 → throw KnowledgeException("模块标识已存在")
  │
  ├─② 构建实体
  │   KnowledgeModuleEntity.builder()
  │     .moduleKey(dto.moduleKey)
  │     .moduleName(dto.moduleName)
  │     .description(dto.description)
  │     .sortOrder(dto.sortOrder != null ? dto.sortOrder : 0)
  │     .questionCount(0)
  │     .status("ONLINE")
  │     .createdAt(NOW())
  │
  └─③ 持久化
      ModulePort.save(entity)
      └→ SQL: INSERT INTO knowledge_modules
           (module_key, module_name, description, status, sort_order, question_count, created_at, updated_at)
           VALUES (?, ?, ?, 'ONLINE', ?, 0, NOW(), NOW())
```

---

### 功能 3：模块上下线

**做什么**：管理员切换模块的 ONLINE / OFFLINE 状态。下线的模块在背诵时不可选。

**完整调用链路**：

```
PUT /admin/knowledge/modules/{moduleKey}/status  {status: "OFFLINE"}

KnowledgeController.updateModuleStatus(moduleKey, dto)
  │
  ├─① 校验模块存在
  │   ModulePort.findByKey(moduleKey)
  │   null → throw KnowledgeException("模块不存在")
  │
  └─② 更新状态
      ModulePort.updateStatus(moduleKey, dto.status)
      └→ SQL: UPDATE knowledge_modules
           SET status=?, updated_at=NOW()
           WHERE module_key=?
```

---

### 功能 4：删除模块

**做什么**：管理员删除模块。

**完整调用链路**：

```
DELETE /admin/knowledge/modules/{moduleKey}

KnowledgeController.deleteModule(moduleKey)
  │
  └─ ModulePort.delete(moduleKey)
      └→ SQL: DELETE FROM knowledge_modules WHERE module_key=?
```

---

### 功能 5：查看模块下题目列表

**做什么**：管理员查看某个模块的所有题目（管理后台用）。

**完整调用链路**：

```
GET /admin/knowledge/modules/{moduleKey}/questions

KnowledgeController.listQuestions(moduleKey)
  │
  ▼
QuestionPort.searchByModule(moduleKey, topK=500)
  │
  ├─① 不需要语义搜索，只需按模块过滤
  │   也可直接调 QuestionPort.findByModule(moduleKey)
  │   这里复用 search("", [moduleKey], 500)
  │
  └→ SQL: SELECT qv.*, 1 - (embedding <=> ?::vector) AS similarity
          FROM question_vectors qv
          WHERE module_key = ?
          LIMIT 500

  返回 List<EmbeddedQuestionVO>
  │
  ▼
Controller 层转换 → List<QuestionManageDTO>
  注: status 字段从 tags 中解析（tags 含 "status:OFFLINE" 则为 OFFLINE，否则 ONLINE）

  └→ Response.ok(List<QuestionManageDTO>)
```

---

### 功能 6：编辑题目

**做什么**：管理员修改题目的题干、答案或所属模块。

**完整调用链路**：

```
PUT /admin/knowledge/questions/{questionId}  {question, content, moduleKey}

KnowledgeController.updateQuestion(questionId, dto)
  │
  ├─① 查当前题目
  │   QuestionPort.getById(questionId)
  │   └→ SELECT * FROM question_vectors WHERE id=?
  │   null → throw KnowledgeException("题目不存在")
  │
  ├─② 更新字段（只更新非空字段）
  │   if (dto.question != null) entity.setQuestion(dto.question)
  │   if (dto.content != null) entity.setContent(dto.content)
  │   if (dto.moduleKey != null) entity.setModuleKey(dto.moduleKey)
  │
  └─③ 重建索引
      QuestionPort.update(entity)
      └→ 内部: deleteById(id) + index(entity)
          (因为改了内容后 embedding 需要重新计算)
```

---

### 功能 7：触发文件导入

**做什么**：管理员触发导入 → 扫描 `docs/import/` 目录 → 解析 JSON/Markdown → 调 embedding API → 写入 question_vectors → 文件移到 backup。

**完整调用链路**：

```
POST /admin/knowledge/import

KnowledgeController.triggerImport()
  │
  ▼
KnowledgeService.importFromFolder()
  │
  ├─① 扫描目录
  │   Files.newDirectoryStream(Path.of("docs/import"))
  │   → 过滤: *.json, *.md
  │
  ├─② 解析文件
  │
  │   .json 文件:
  │   Gson 解析 [{question, content, module_key, category, tags, difficulty}]
  │   每项构建 QuestionEntity
  │
  │   .md 文件:
  │   按 "## " 分段 → 首行为题目标题 → 余为答案正文
  │   moduleKey 从文件名推导: "java基础.md" → "java-basics"
  │   difficulty 按答案长度估算: <200=1, <800=2, <2000=3, <5000=4, ≥5000=5
  │   每段构建 QuestionEntity
  │
  ├─③ 批量获取向量
  │   收集所有 content → EmbeddingPort.embedBatch(contents)
  │   │
  │   └→ OkHttp POST https://api.siliconflow.cn/v1/embeddings
  │       Body: {model:"Qwen/Qwen3-Embedding-0.6B", input: [content1, content2, ...]}
  │       Response: [{embedding: [0.123, ...1024个]}, ...]
  │   返回 List<float[1024]>
  │
  ├─④ 逐题入库
  │   每个 QuestionEntity 设置 embedding → QuestionPort.index(entity)
  │   │
  │   └→ SQL: INSERT INTO question_vectors
  │        (id, content, question, module_key, category, tags, difficulty, embedding)
  │        VALUES (UUID, ?, ?, ?, ?, ?, ?, ?::vector)
  │
  ├─⑤ 同步模块题目计数
  │   按 moduleKey 分组统计 → ModulePort.updateQuestionCount(moduleKey, delta)
  │   └→ SQL: UPDATE knowledge_modules SET question_count = question_count + ?
  │        WHERE module_key = ?
  │
  └─⑥ 文件归档
      Files.move(file, backupDir.resolve(filename + "_" + timestamp))
      → 移到 docs/import/backup/

  返回 {imported: N, message: "已导入 N 题"}
```

---

### 功能 8：语义搜索（被 recite 子域同步调用）

**做什么**：根据查询文本和模块范围，返回最相似的题目。这是 recite 子域出题的唯一入口。

**完整调用链路**：

```
由 ReciteOrchestrationService.startRecite() 调用（Phase 5）:

QuestionPort.search(query, moduleKeys, topK)
  │
  ├─① 文本转向量
  │   EmbeddingPort.embed(query)
  │   └→ SiliconFlow API → float[1024]
  │
  ├─② pgvector 语义搜索
  │   └→ SQL:
  │       SELECT qv.*, 1 - (embedding <=> ?::vector) AS similarity
  │       FROM question_vectors qv
  │       WHERE module_key = ANY(?)
  │         AND tags NOT LIKE '%status:OFFLINE%'
  │       ORDER BY embedding <=> ?::vector
  │       LIMIT ?
  │
  │   <=> 是余弦距离运算符，1-距离 = 余弦相似度 (0~1)
  │   HNSW 索引自动加速（m=48, ef_construction=200）
  │
  └─③ 组装返回
      List<EmbeddedQuestionVO> = each row → {QuestionEntity, similarity}
      按 similarity DESC 排序

  返回给 recite 子域 → 从中选取题目出题
```

---

## 三、涉及数据库表

| 表 | 操作 | 关键 SQL |
|---|---|---|
| `knowledge_modules` | CRUD | 标准 MyBatis Plus |
| `question_vectors` | 增删改查 + 向量搜索 | `embedding <=> ?` 余弦距离 |
| 外部 API | SiliconFlow Embedding | `Qwen3-Embedding-0.6B`，1024 维 |

---

## 四、类清单与关系

```
┌──────────────────────────────────────────────────────────────────┐
│                   recite-api (REST 契约)                          │
│                                                                   │
│  IKnowledgeService    ← 7 个 REST 端点定义                        │
│  DTO × 7              ← 请求/响应 传输对象                        │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│                   recite-trigger (控制器)                          │
│                                                                   │
│  KnowledgeController   ← 实现 IKnowledgeService                   │
│                          调用 Port 接口，Entity ↔ DTO 转换         │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 依赖注入 (3 个 Port + 1 个 Service)
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│                recite-domain/knowledge (领域层)                    │
│                                                                   │
│  model/entity/                                                    │
│  ├─ KnowledgeModuleEntity   实体：模块                            │
│  └─ QuestionEntity          实体：题目（含 1024 维向量）           │
│                                                                   │
│  model/valueobj/                                                  │
│  └─ EmbeddedQuestionVO      record：QuestionEntity + similarity   │
│                                                                   │
│  port/out/ (SPI 接口 — infra 层实现)                               │
│  ├─ ModulePort        模块 CRUD                                   │
│  ├─ QuestionPort      向量搜索 + 题目 CRUD  ★ 后续核心依赖         │
│  └─ EmbeddingPort     文本 → 向量                                 │
│                                                                   │
│  service/                                                         │
│  └─ KnowledgeService  编排导入流程                                │
│                                                                   │
│  exception/                                                       │
│  └─ KnowledgeException  子域异常                                  │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│             recite-infrastructure/adapter (基础设施)               │
│                                                                   │
│  persistence/                                                     │
│  ├─ ModuleManagementAdapter    implements ModulePort               │
│  │     └→ KnowledgeModuleDO + KnowledgeModuleMapper               │
│  │                                                                 │
│  rag/                                                             │
│  └─ PgVectorAdapter           implements QuestionPort              │
│        └→ QuestionVectorDO + QuestionVectorMapper                 │
│        └→ 自定义 SQL: embedding <=> query_vec                     │
│                                                                   │
│  embedding/                                                       │
│  └─ SiliconFlowEmbeddingAdapter  implements EmbeddingPort          │
│        └→ OkHttp → SiliconFlow API → float[1024]                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 五、每个类/接口的详细职责

### 5.1 领域实体（Entity）

#### KnowledgeModuleEntity
```java
// 字段
Long id;
String moduleKey;      // 唯一标识 "java-basics"
String moduleName;     // 展示名 "Java基础"
String description;    // 描述
String status;         // ONLINE / OFFLINE
Integer sortOrder;     // 排序权重
Integer questionCount; // 题目数量（冗余计数，导入/删除时同步更新）
LocalDateTime createdAt;
LocalDateTime updatedAt;
```

#### QuestionEntity
```java
// 字段
String id;            // UUID
String question;      // 题目标题，最长 2000
String content;       // 答案正文，最长 TEXT
String moduleKey;     // 所属模块
String category;      // 分类标签
String tags;          // 标签字符串（空格分隔）
Integer difficulty;   // 1-5
float[] embedding;    // 1024 维向量
```

### 5.2 值对象（Value Object）

#### EmbeddedQuestionVO
```java
// record，不可变
public record EmbeddedQuestionVO(
    QuestionEntity question,
    Double similarityScore  // 余弦相似度，0-1
) {}
```

### 5.3 Port 接口（SPI，领域层定义）

#### ModulePort
```java
public interface ModulePort {
    List<KnowledgeModuleEntity> listAll();                    // 全部模块，按 sortOrder 排序
    List<KnowledgeModuleEntity> listOnline();                 // 仅 ONLINE 状态
    Optional<KnowledgeModuleEntity> findByKey(String moduleKey); // 按 key 查
    void save(KnowledgeModuleEntity module);                  // 新增
    void updateStatus(String moduleKey, String status);       // 上下线
    void updateQuestionCount(String moduleKey, int delta);    // 题目计数增减
    void delete(String moduleKey);                            // 删除模块
}
```

#### QuestionPort
```java
public interface QuestionPort {
    // 语义搜索 — 面向背诵的核心出题方法
    List<EmbeddedQuestionVO> search(String query, List<String> moduleKeys, int topK);

    // 按模块列出（管理后台用）
    List<EmbeddedQuestionVO> searchByModule(String moduleKey, int topK);

    // 题目管理
    void index(QuestionEntity question);       // 新增/重建索引（含 embedding）
    void deleteById(String questionId);        // 删除
    void update(QuestionEntity question);       // 更新（先删后插）
    QuestionEntity getById(String questionId); // 按 ID 查
    boolean hasData();                         // 题库是否有数据

    // 计数查询
    int countByModule(String moduleKey);       // 某模块题目数
}
```

#### EmbeddingPort
```java
public interface EmbeddingPort {
    float[] embed(String text);                    // 单条文本 → 向量
    List<float[]> embedBatch(List<String> texts);  // 批量文本 → 向量列表
}
```

### 5.4 领域服务

#### KnowledgeService
```java
public class KnowledgeService {
    // 依赖: EmbeddingPort, QuestionPort

    /**
     * 导入流程编排（不调外部 API）：
     * 1. 扫描 docs/import/ 目录下 .json / .md 文件
     * 2. 解析文件提取题目列表
     * 3. 批量调 EmbeddingPort.embedBatch() 获取向量
     * 4. 逐题调 QuestionPort.index() 入库
     * 5. 更新对应模块的 questionCount
     * 6. 文件移到 docs/import/backup/
     *
     * @return 成功导入的题目数量
     */
    int importFromFolder();
}
```

### 5.5 REST 接口

#### IKnowledgeService
```
路径前缀: /admin/knowledge
```

| # | 方法 | HTTP | 路径 | 请求体 | 返回 |
|:--:|------|------|------|------|------|
| 1 | `listModules` | GET | `/modules` | — | `Response<List<KnowledgeModuleDTO>>` |
| 2 | `createModule` | POST | `/modules` | `ModuleCreateRequestDTO` | `Response<Void>` |
| 3 | `updateModuleStatus` | PUT | `/modules/{key}/status` | `ModuleStatusRequestDTO` | `Response<Void>` |
| 4 | `deleteModule` | DELETE | `/modules/{key}` | — | `Response<Void>` |
| 5 | `listQuestions` | GET | `/modules/{key}/questions` | — | `Response<List<QuestionManageDTO>>` |
| 6 | `updateQuestion` | PUT | `/questions/{id}` | `QuestionEditDTO` | `Response<Void>` |
| 7 | `triggerImport` | POST | `/import` | — | `Response<ImportResultDTO>` |

### 5.6 DTO（7 个）

| DTO | 字段 | 用途 |
|---|---|---|
| `KnowledgeModuleDTO` | id, moduleKey, moduleName, description, status, sortOrder, questionCount | 模块列表响应 |
| `ModuleCreateRequestDTO` | moduleKey, moduleName, description, sortOrder | 创建模块请求 |
| `ModuleStatusRequestDTO` | status (ONLINE/OFFLINE) | 状态变更请求 |
| `QuestionDTO` | id, question, content, moduleKey, category, tags, difficulty, similarityScore | 搜索响应（背诵页用） |
| `QuestionManageDTO` | id, question, content, moduleKey, category, tags, difficulty, status | 题目列表响应（管理页用） |
| `QuestionEditDTO` | question, content, moduleKey | 编辑题目请求 |
| `ImportResultDTO` | imported (int), message (String), errors (List\<String\>) | 导入结果响应 |

### 5.7 基础设施适配器

| 适配器 | 实现接口 | 技术要点 |
|---|---|---|
| `ModuleManagementAdapter` | `ModulePort` | MyBatis Plus `LambdaQueryWrapper`，操作 `knowledge_modules` 表 |
| `PgVectorAdapter` | `QuestionPort` | 自定义 SQL，pgvector `<=>` 余弦距离，HNSW 索引自动使用 |
| `SiliconFlowEmbeddingAdapter` | `EmbeddingPort` | OkHttp，POST `https://api.siliconflow.cn/v1/embeddings`，model: `Qwen/Qwen3-Embedding-0.6B` |

### 5.8 持久层（DO + Mapper）

| 类 | 映射表 | 说明 |
|---|---|---|
| `KnowledgeModuleDO` | `knowledge_modules` | MyBatis Plus PO，字段与表列一一对应 |
| `KnowledgeModuleMapper` | 同上 | `extends BaseMapper<KnowledgeModuleDO>` |
| `QuestionVectorDO` | `question_vectors` | PO 含 `float[] embedding` 字段（pgvector 映射） |
| `QuestionVectorMapper` | 同上 | `extends BaseMapper<QuestionVectorDO>` + 自定义 `@Select` 向量搜索 SQL |

### 5.9 控制器

#### KnowledgeController
- 实现 `IKnowledgeService`
- 注入 `ModulePort`、`QuestionPort`、`KnowledgeService`
- 只做参数校验 + Entity↔DTO 转换，业务逻辑在领域层
- 异常由 `GlobalExceptionHandler` 统一拦截

---

## 六、依赖注入关系

```
KnowledgeController
  ├─ @Autowired ModulePort       → ModuleManagementAdapter
  ├─ @Autowired QuestionPort     → PgVectorAdapter
  └─ @Autowired KnowledgeService
       ├─ @Autowired EmbeddingPort  → SiliconFlowEmbeddingAdapter
       └─ @Autowired QuestionPort   → PgVectorAdapter (同实例)
```

所有 Port 按类型自动装配（Spring `@Autowired`），各子域只有一个实现类。

---

## 七、pgvector 关键 SQL

```sql
-- 语义搜索（PgVectorAdapter.search 核心查询）
SELECT qv.*, 1 - (embedding <=> ?::vector) AS similarity
FROM question_vectors qv
WHERE module_key = ANY(?)
ORDER BY embedding <=> ?::vector
LIMIT ?;

-- 插入向量（PgVectorAdapter.index）
INSERT INTO question_vectors (id, content, question, module_key, category, tags, difficulty, embedding)
VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector);

-- 模块题目计数
SELECT COUNT(*) FROM question_vectors WHERE module_key = ?;
```

HNSW 索引已在 schema.sql 中创建（`m=48, ef_construction=200`），查询时自动使用。

---

## 八、异常处理

```java
// KnowledgeException 继承 AppException
// 场景：
//   - 模块 key 重复 → "模块标识已存在"
//   - 模块不存在 → "模块不存在"
//   - 导入文件格式错误 → "文件格式不正确"
//   - embedding 失败 → "向量化失败"
```

---

## 九、文件清单

```
recite-v2/
│
├── recite-api/src/main/java/cn/bugstack/recite/api/
│   ├── IKnowledgeService.java                    ← REST 接口，7 个端点
│   └── dto/
│       ├── KnowledgeModuleDTO.java
│       ├── ModuleCreateRequestDTO.java
│       ├── ModuleStatusRequestDTO.java
│       ├── QuestionDTO.java
│       ├── QuestionManageDTO.java
│       ├── QuestionEditDTO.java
│       └── ImportResultDTO.java
│
├── recite-domain/src/main/java/cn/bugstack/recite/domain/knowledge/
│   ├── model/entity/
│   │   ├── KnowledgeModuleEntity.java
│   │   └── QuestionEntity.java
│   ├── model/valueobj/
│   │   └── EmbeddedQuestionVO.java
│   ├── port/out/
│   │   ├── ModulePort.java
│   │   ├── QuestionPort.java
│   │   └── EmbeddingPort.java
│   ├── service/
│   │   └── KnowledgeService.java
│   └── exception/
│       └── KnowledgeException.java
│
├── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/adapter/
│   ├── persistence/
│   │   ├── KnowledgeModuleDO.java
│   │   ├── KnowledgeModuleMapper.java
│   │   ├── QuestionVectorDO.java
│   │   ├── QuestionVectorMapper.java
│   │   └── ModuleManagementAdapter.java
│   ├── rag/
│   │   └── PgVectorAdapter.java
│   └── embedding/
│       └── SiliconFlowEmbeddingAdapter.java
│
└── recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/
    └── KnowledgeController.java
```

**总计 20 个文件**（8 api + 8 domain + 3 infra + 1 trigger）

---

## 十、编码顺序（建议）

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `KnowledgeException.java` | 其他类依赖 |
| 2 | Entity + VO（3 个） | 纯数据结构 |
| 3 | Port 接口（3 个） | 领域契约 |
| 4 | `KnowledgeService.java` | 领域服务，依赖 Port |
| 5 | DTO（7 个）+ `IKnowledgeService.java` | API 契约 |
| 6 | DO + Mapper（4 个） | 持久层 |
| 7 | Adapter（3 个） | 实现 Port |
| 8 | `KnowledgeController.java` | 最后组装 |

每步完成后 `mvn compile` 验证。
