# 背诵助手 V2 实施计划

> 日期：2026-06-12 | 状态：Phase 0-2 完成，Phase 3 待开始

---

## 一、总览

| # | Phase | 内容 | 产出 | 依赖 |
|:--:|------|------|------|------|
| 0 | 脚手架 | 六模块骨架 + POM + 配置 | 16 文件 | — |
| 1 | 底层基础 | types + api 基类 | 4 文件 | 0 |
| 2 | 目录约定 | 五子域 port/out 端口 | 目录结构 | 0 |
| **3** | **knowledge** | **题目搜索 + 模块管理 + 导入** | **~8 文件** | **1** |
| 4 | auth | 登录/注册 + UserContext | ~6 文件 | 1 |
| 5 | recite | 会话 + 评分 + SSE | ~10 文件 | 3, 4 |
| 6 | progress | 间隔重复 + 连续天数 | ~8 文件 | 5 |
| 7 | report | 报告统计 + MQ 异步 | ~8 文件 | 5, 6 |
| 8 | achievement | 徽章策略 + API | ~12 文件 | 5, 6 |
| 9 | 可观测性 | 链路追踪 + 运维后台 | ~10 文件 | 5-8 |
| 10 | 前端 | Pinia + ChatRecite + AchievementWall + AdminMonitor | ~15 文件 | 4-9 |
| 11 | 测试 | 单元 + 集成 + E2E | ~10 文件 | 5-8 |

---

## 二、架构决策（已定）

| 决策 | 结论 |
|------|------|
| 模块结构 | DDD 六模块（types/api/domain/infrastructure/trigger/app） |
| 领域划分 | 五子域（recite/progress/report/achievement/knowledge）|
| SPI 位置 | 每个子域 `port/out/` 包 |
| 子域通信 | RocketMQ 消息，不使用共享内核/DomainEvent |
| 数据库 | PostgreSQL + pgvector，弃用 Milvus |
| 缓存 | Redis (Redisson)，会话状态 + 评分并发信号量 |
| 消息队列 | RocketMQ，报告异步生成 |
| 认证 | Sa-Token JWT + UserContext (TTL) |
| ORM | MyBatis Plus |
| 前端状态 | Pinia |
| API 文档 | Swagger 3 (springdoc) |
| 测试报告 | Allure |
| 会话存储 | Redis，TTL 2 小时 |
| 架构守护 | ArchUnit（后续加） |

---

## 三、数据存储分工

| 数据 | 存储 | 过期 |
|------|------|:--:|
| 题目向量 | `question_vectors` (pgvector) | — |
| 用户/记录/进度 | `users`, `recite_records`, `user_progress` | — |
| 学习档案 | `learning_journal` | — |
| 连续天数 | `user_streak` | — |
| 成就记录 | `achievement_log` | — |
| 链路追踪 | `trace_runs`, `trace_nodes` | 30 天 |
| 背诵会话 | Redis `recite:session:{sid}` | 2h |
| 评分并发 | Redis `recite:score:slots` | — |
| 报告任务 | RocketMQ `recite-report-topic` | — |

---

## 四、新增数据库表

| 表 | 阶段 | 用途 |
|------|:--:|------|
| `question_vectors` | 3 | pgvector 向量搜索（替代 Milvus collection）|
| `user_streak` | 6 | 连续背诵天数 |
| `learning_journal` | 7 | LLM 报告摘要归档 |
| `achievement_log` | 8 | 徽章获得记录 |
| `trace_runs` | 9 | 链路追踪根节点 |
| `trace_nodes` | 9 | 链路追踪节点明细 |

已有表保留（`users`, `admin_users`, `knowledge_modules`, `recite_records`, `user_progress`），字段按需调整。

---

## 五、Docker Compose

```
改造前: PG + etcd + MinIO + Milvus = 4 容器
改造后: PG(pgvector) + Redis + RocketMQ = 3 容器
```

---

## 六、已完成文件清单

```
E:\xiangmu\beisong\recite-v2\
├── pom.xml
├── .gitignore
├── recite-types/pom.xml
│   └── src/main/java/cn/bugstack/recite/types/
│       ├── common/Constants.java
│       ├── enums/ResponseCode.java
│       ├── enums/ReciteMode.java
│       └── exception/AppException.java
├── recite-api/pom.xml
│   └── src/main/java/cn/bugstack/recite/api/response/Response.java
├── recite-domain/pom.xml
│   └── src/main/java/cn/bugstack/recite/domain/
│       ├── recite/port/out/.gitkeep
│       ├── progress/port/out/.gitkeep
│       ├── report/port/out/.gitkeep
│       ├── achievement/port/out/.gitkeep
│       └── knowledge/port/out/.gitkeep
├── recite-infrastructure/pom.xml
├── recite-trigger/pom.xml
└── recite-app/pom.xml
    └── src/main/java/cn/bugstack/recite/app/
        ├── Application.java
        └── resources/
            ├── application.yml
            └── application-dev.yml
```

---

## 七、Git

```
仓库: https://github.com/zwztms/recite
分支: main
本地 commits:
  65cc189 revert: 砍掉共享内核，子域间直接用MQ通信
  ae241ae scaffold: 五子域 port/out 端口目录约定
  18bd132 scaffold: 共享内核尝试（已回退）
  9658b1c scaffold: 六模块骨架 + types基础 + POM依赖 + 应用配置
```

---

## 八、各 Phase 详细设计引用

| Phase | 设计文档章节 |
|:--:|------|
| 3 knowledge | spec §二（核心约束）、§四（消息流程） |
| 4 auth | spec §二（核心约束）、Ragent UserContext 模式 |
| 5 recite | spec §四-六（消息流、SSE、ChatInput）、§十一（追问） |
| 6 progress | spec §十（间隔重复）、§十二（时间系统） |
| 7 report | spec §八（报告设计）、§九（学习档案）、§十七（MQ） |
| 8 achievement | spec §十三（成就徽章） |
| 9 可观测性 | spec §十八（可观测性）、§十九（API 文档） |
| 10 前端 | spec §三-四（视觉、消息流）、§二十（Pinia）、样例文件 |
| 11 测试 | spec §二十二（测试策略） |

完整设计文档：`E:\xiangmu\beisong\recite\docs\superpowers\specs\2026-06-11-chat-recite-design.md`
