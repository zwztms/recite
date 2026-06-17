# 编码规范

> 所有新代码必须遵守。

---

## 包结构约定

```
domain/{子域}/
├── model/
│   ├── entity/       ← 实体（有 ID，可变）
│   ├── valueobj/     ← 值对象（无 ID，不可变，用 record）
│   └── event/        ← 领域事件（仅 MQ 消息体，不用 DomainEvent 接口）
├── port/
│   └── out/          ← SPI 接口（领域层定义，infra 实现）
├── service/          ← 领域服务（纯业务规则，无状态，不调外部）
└── exception/        ← 子域专属异常

infrastructure/
└── adapter/
    ├── persistence/  ← MyBatis Mapper + DO 对象
    ├── rag/          ← pgvector 搜索
    ├── llm/          ← DeepSeek API
    ├── embedding/    ← SiliconFlow API
    ├── cache/        ← Redis
    └── mq/           ← RocketMQ consumer

trigger/
├── http/             ← Controller
└── config/           ← Interceptor, WebMvcConfig
```

## 命名规则

| 类型 | 命名 | 示例 |
|------|------|------|
| 实体 | `XxxEntity` | `QuestionEntity` |
| 值对象 | `XxxVO` | `EmbeddedQuestionVO` |
| Port 接口 | `XxxPort` | `QuestionQueryPort` |
| Port 实现 | `XxxAdapter` | `PgVectorAdapter` |
| 领域服务 | `XxxService` | `ReciteOrchestrationService` |
| Controller | `XxxController` | `ReciteController` |
| DO 对象 | `XxxDO` | `ReciteRecordDO` |
| Mapper | `XxxMapper` | `ReciteRecordMapper` |
| DTO | `XxxDTO` | `QuestionDTO` |

## 异常处理

- 领域层抛 `DomainException`（继承 `cn.bugstack.recite.types.exception.AppException`）
- Controller 层不 try-catch，由 `GlobalExceptionHandler` 统一拦截
- 子域专属异常继承 `DomainException`

## 依赖注入

- Port 接口在 domain 层定义，用 `@Autowired` 注入（Spring 按类型自动装配）
- 实现类在 infrastructure 层加 `@Service` 或 `@Component`

## 零依赖规则

- domain 层不依赖 Spring、MyBatis、Redis、RocketMQ
- domain 层只依赖 `recite-types` 和 JDK
- 所有外部 API 调用在 infrastructure 层

## Git 提交

- 格式：`phase{N}: {简短描述}`
- 每完成一个 Phase 至少一次 commit
- 不跨 Phase 混合提交
