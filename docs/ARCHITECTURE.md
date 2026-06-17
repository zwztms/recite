# 架构文档

> 所有已确定的架构决策，新代码必须遵守。

---

## 六模块依赖

```
app → trigger → domain → types
app → infra → domain
trigger 不依赖 infra（只通过 domain port 接口调用）
```

## 五子域

| 子域 | 包 | 职责 | 类型 |
|------|------|------|:--:|
| recite | `domain/recite/` | 会话、评分、追问 | 核心域 |
| progress | `domain/progress/` | 掌握度、间隔重复、连续天数 | 核心域 |
| report | `domain/report/` | 报告生成、学习档案 | 支撑域 |
| achievement | `domain/achievement/` | 徽章定义、发放 | 支撑域 |
| knowledge | `domain/knowledge/` | 题目搜索、模块管理、导入 | 支撑域 |

子域间通过 **RocketMQ** 通信，不直接依赖。recite 是编排中心。

## Port 约定

每个子域下必须有 `port/out/` 包，放 SPI 接口。实现放 `recite-infrastructure/adapter/` 下。

## 基础设施 adapter 组织

```
infrastructure/adapter/
├── persistence/    ← MyBatis Mapper + DO
├── rag/            ← pgvector 搜索
├── llm/            ← DeepSeek API
├── embedding/      ← SiliconFlow API
├── cache/          ← Redis
├── mq/             ← RocketMQ consumer
└── trace/          ← 链路追踪
```

## 数据存储分工

| 数据 | 存储 | Key | 过期 |
|------|------|------|:--:|
| 题目向量 | PG `question_vectors` | — | — |
| 业务数据 | PG `users/records/progress/...` | — | — |
| 背诵会话 | Redis | `recite:session:{sid}` | 2h |
| 评分并发 | Redis | `recite:score:slots` (10) | — |
| 报告任务 | RocketMQ | `recite-report-topic` | — |

## 容器

```
PG(pgvector) + Redis + RocketMQ = 3 容器
(旧: PG + etcd + MinIO + Milvus = 4)
```

## 核心约束

1. 所有背诵题目来自题库 `question_vectors` 表
2. **追问是唯一例外**——LLM 现场生成，不在题库
3. 子域间用 MQ，不用共享内核
4. port/out 放 SPI 接口，infrastructure 实现
5. 不用 JPA，用 MyBatis Plus

## 技术栈版本

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.4.3 |
| MyBatis Plus | 3.5.14 |
| Sa-Token | 1.43.0 |
| pgvector | PG 扩展 |
| Redisson | 3.41.0 |
| RocketMQ | 5.2.0 |
| SpringDoc | 2.7.0 |
