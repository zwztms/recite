# 背诵助手 V2 — 开发上下文

> 加载此文件到新 session 即可恢复全部设计上下文。
> 详细设计：`E:\xiangmu\beisong\recite\docs\superpowers\specs\2026-06-11-chat-recite-design.md`

---

## 1. 项目定位

八股文背诵助手 V2 完全重写。新代码在 `E:\xiangmu\beisong\recite-v2\`，旧代码 `recite\` 不动。
Git: `https://github.com/zwztms/recite`，分支 `main`。

---

## 2. 技术栈

| 组件 | 选型 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.4.3 |
| ORM | MyBatis Plus 3.5.14 |
| 认证 | Sa-Token JWT 1.43.0 |
| 数据库 | PostgreSQL + pgvector 插件 |
| 向量检索 | pgvector HNSW 索引，COSINE 相似度（替代 Milvus） |
| 缓存 | Redis (Redisson) |
| 消息队列 | RocketMQ |
| 前端 | Vue 3 + Vite + Pinia + Tailwind CSS v4 |
| 工具 | Hutool, OkHttp, Gson, Lombok |
| 测试 | JUnit 5 + AssertJ + Allure |
| API 文档 | Swagger 3 (springdoc) |
| LLM | DeepSeek Chat API（评分/报告） |
| Embedding | SiliconFlow Qwen3-Embedding-0.6B, 1024 维 |

---

## 3. 架构

### 3.1 模块结构

```
recite-v2/
├── recite-types/          基础定义（被所有人依赖）
├── recite-api/            DTO + REST 接口（依赖 types）
├── recite-domain/         领域层（依赖 types + TTL）
│   ├── shared/            （已砍掉，不用）
│   ├── domain/recite/     背诵子域
│   ├── domain/progress/   进度子域
│   ├── domain/report/     报告子域
│   ├── domain/achievement/成就子域
│   └── domain/knowledge/  知识库子域
├── recite-infrastructure/ 基础设施层（依赖 domain）
│   └── adapter/           所有实现按 adapter 组织
├── recite-trigger/        控制器 + 拦截器（依赖 domain + api）
└── recite-app/            启动入口 + 配置（依赖 trigger + infra）
```

**依赖规则**: trigger 不直接依赖 infrastructure，只通过 domain 的 port 接口调用。

### 3.2 子域划分

| 子域 | 职责 | 类型 |
|------|------|:--:|
| recite | 会话生命周期、评分编排、追问 | 核心域 |
| progress | 掌握度、间隔重复、连续天数 | 核心域 |
| report | 报告生成、学习档案 | 支撑域 |
| achievement | 徽章定义、发放 | 支撑域 |
| knowledge | 题目搜索、模块管理、导入 | 支撑域 |

子域间通过 RocketMQ 通信，不使用共享内核/DomainEvent 基类。

每个子域下必须有 `port/out/` 包，放 SPI 接口。基础设施层 `adapter/` 下实现。

### 3.3 容器

```
Docker Compose: PG(pgvector) + Redis + RocketMQ = 3 容器
（改造前: PG + etcd + MinIO + Milvus = 4 容器）
```

---

## 4. 数据库

Schema: `E:\xiangmu\beisong\recite-v2\docs\dev-ops\sql\schema.sql`
ER 图: `E:\xiangmu\beisong\项目架构.drawio`（子 agent 正在更新）

11 张表:

| 表 | 类型 | 用途 |
|------|:--:|------|
| users | 保留 | 用户 |
| admin_users | 保留 | 管理员 |
| knowledge_modules | 保留 | 模块管理 |
| question_vectors | **新** | pgvector 向量搜索 |
| recite_records | 修改 | +follow_up_depth, parent_record_id, response_time_seconds |
| user_progress | 修改 | +mastery_score(0-100), next_review_at, review_interval, ease_factor |
| learning_journal | **新** | LLM 报告归档 |
| user_streak | **新** | 连续天数 |
| achievement_log | **新** | 徽章记录 |
| trace_runs | **新** | 链路追踪 |
| trace_nodes | **新** | 链路节点 |

---

## 5. 数据存储分工

| 数据 | 存储 | Key | 过期 |
|------|------|------|:--:|
| 题目向量 | PG | question_vectors | — |
| 用户/记录/进度 | PG | users, recite_records, user_progress | — |
| 学习档案 | PG | learning_journal | — |
| 连续天数 | PG | user_streak | — |
| 成就记录 | PG | achievement_log | — |
| 链路追踪 | PG | trace_runs, trace_nodes | 30天 |
| 背诵会话 | Redis | recite:session:{sid} | 2h |
| 评分并发 | Redis | recite:score:slots (Semaphore, 10) | — |
| 报告任务 | RocketMQ | recite-report-topic | — |

---

## 6. 已确认的功能设计

### 6.1 对话交互
- AI 消息无气泡纯文本，用户消息隐约橘色 pill，无头像
- 暖灰底色 + 橘珊瑚 #f97316 点缀
- Enter 发送，Shift+Enter 换行，IME 防误触，自动增高 max 120px
- SSE 分 5 段流式推送评分（不改 LLM 调用逻辑）
- 样例: `recite\docs\superpowers\specs\mockup-v2.html`

### 6.2 三种背诵模式
| 模式 | 出题 | 评分 | 题数 |
|------|------|------|:--:|
| 模块背诵 | 指定模块 | AI 1-10 分 | 用户选 |
| 随机背诵 | 跨模块随机 | AI 1-10 分 | 用户选 |
| 今日复习 | 到期题（背过的）| 自评（想起/不确定/忘了）| 固定 10 |

### 6.3 间隔重复
- mastery_score: 0-100（替代枚举）
- 标记：interval × ease（高分）/ reset 1day（低分）
- 今日复习：自评驱动，不调 LLM，不出新题

### 6.4 报告系统
- 仪表盘: 4 栏统计 + 模块横向条 + 趋势 + 薄弱标签 + AI 建议
- Java 汇总数字 / LLM 写评语，比例约 45/55
- learning_journal 存 LLM 摘要，下次注入最近 5 次到 prompt
- 报告通过 MQ 异步生成，前端轮询 GET /{sid}/report

### 6.5 成就徽章
- 46 枚（5 类 + 11 隐藏）
- 卡片列表 + 已获得金色 icon + 未获得灰显进度
- 获得时顶部 toast 3 秒
- 样例: `recite\docs\superpowers\specs\mockup-v2.html`

### 6.6 追问链
- 最多 3 层，LLM 现场生成（不在题库中）
- 用户许可后才追问
- 仅附报告回忆，不评分，不入 user_progress

### 6.7 核心约束
- 所有背诵题目来自题库（`question_vectors` 表）
- 追问是唯一例外

---

## 7. 可观测性

- 注解式链路追踪: @ReciteTraceRoot / @ReciteTraceNode → AOP 写 trace_runs + trace_nodes
- GlobalExceptionHandler: ClientException / ServiceException
- Swagger: /doc.html
- 管理后台监控: AdminMonitor.vue（样例: `recite\docs\superpowers\specs\mockup-admin-monitor.html`）

---

## 8. 错误处理

- AppException → ClientException（4xx）/ ServiceException（5xx）
- @RestControllerAdvice 统一拦截
- 前端 axios 拦截器统一处理 401/500
- SSE 断连: 显示"连接断开，请重试"

---

## 9. 前端

- Pinia: 3 个 Store（recite / achievement / auth）
- 页面: ChatRecite（合并 Category+Random+Review）/ AchievementWall / AdminMonitor
- 路由: /recite /achievements /admin/modules /admin/monitor
- 样例: `recite\docs\superpowers\specs\mockup-v2.html`
- Tailwind 样例: `E:\xiangmu\beisong\recite\.superpowers\...\v2-frontend.html`

---

## 10. 测试策略

- 三层: 单元(JUnit+AssertJ) / 集成(MockMvc) / E2E(TestRestTemplate)
- P0: 间隔算法 / ReportService.aggregate() / AchievementService / 关键 API
- 报告: Allure

---

## 11. 实施计划

| Phase | 内容 | 状态 |
|:--:|------|:--:|
| 0 | 脚手架 + POM | ✅ |
| 1 | types + api 基类 | ✅ |
| 2 | port/out 目录 | ✅ |
| 3 | knowledge 子域 | 待开始 |
| 4 | auth 认证 | — |
| 5 | recite 背诵核心 | — |
| 6 | progress 进度 | — |
| 7 | report 报告 | — |
| 8 | achievement 成就 | — |
| 9 | 可观测性 | — |
| 10 | 前端 | — |
| 11 | 测试 | — |

详细计划: `E:\xiangmu\beisong\recite-v2\docs\superpowers\plans\2026-06-12-v2-implementation-plan.md`

---

## 12. 关键文件索引

| 文件 | 内容 |
|------|------|
| `recite-v2/docs/dev-ops/sql/schema.sql` | 11 张表完整 DDL |
| `recite-v2/docs/superpowers/plans/2026-06-12-v2-implementation-plan.md` | 实施总计划 |
| `recite\docs\superpowers\specs\2026-06-11-chat-recite-design.md` | V2 完整设计文档（旧路径，22章35需求） |
| `E:\xiangmu\beisong\项目架构.drawio` | 架构图（子 agent 更新中） |
| `recite-v2/` | 新项目根目录 |
| `recite\docs\superpowers\specs\mockup-v2.html` | 前端样例 |
| `recite\docs\superpowers\specs\mockup-admin-monitor.html` | 运维后台样例 |

---

## 13. Git 状态

- 仓库: https://github.com/zwztms/recite
- 本地 4 commits（后 2 个因网络未推）
- 第 3 个 commit 已被 reset 回退，不需关注

---

## 14. 下一步

1. 等子 agent 完成架构图更新
2. 按 Phase 3-11 逐 Phase 写详细计划（接口/表/依赖）
3. 从 Phase 3 开始编码
