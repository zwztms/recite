# 八股文背诵助手 V2 — Q&A 全文记录

> 按时间顺序，完整记录所有项目相关的问答讨论。共约 15 个主题。

---

## 1. 项目理解阶段

**Q: 先了解一下整体**
→ 介绍了项目结构、技术栈（Java 17 / Spring Boot 3.4.3 / DDD 六模块 / Milvus + PostgreSQL / Vue 3）、CLAUDE.md、对象调用关系、V2 功能设计、部署文档、账号等。

**Q: 对我的开发流程清楚吗,对当前项目的痛点和bug了解吗**
→ 从记忆文件读取了开发习惯（先列文件清单、编译验证、step{N} 提交、多 agent 并行）、已知 bug（sessionQuestions 单例共享 Map、会话隔离隐患、Hook 注入未启用、V2 功能待开发）。

**Q: 首先说明当前的需求,当前的背诵模块是用户输入到提交方块中,我需要实现类似与网站deepseek的网页对话框**
→ 确认需求：对话框形式替代 textarea+按钮。开始探索现有背诵代码和 ragent 参考实现。

---

## 2. 对话框设计

**Q: （选择交互模式）**
A: 提出三个选项——A 严格顺序 / B 稍微放松 / C 完全自由聊天。用户选 A。

**Q: 其次当前的界面设计太难看,多使用一些暖色调**
→ 展示了三种暖色方案：琥珀暖金、橘珊瑚 Coral、暖玫瑰。用户选橘珊瑚 #f97316。

**Q: （报告展示方式）**
A: 三个选项——A 特殊 AI 消息气泡 / B 弹出模态框 / C 切换独立视图。
用户: 总报告可以在对话流末尾查看,也要在用户个人页面可以查看 → 即 A + 历史页。

**Q: 输入框的交互方式**
A: A DeepSeek 风格（Enter 发送/Shift+Enter 换行/自动增高/IME 兼容）/ B 普通 textarea+按钮。选 A。

**Q: 评分结果要不要做流式输出**
A: A SSE 流式 / B 一次性返回。用户选 A。
追问：需要进行多大范围的修改,实现复杂度如何。
A: 后端不改核心逻辑，Controller 新增 SseEmitter，分 5 段延迟推送（400ms 间隔）。改动清单：ReciteController 中等改、DeepSeekLlmGateway 不改、前端大改（ChatInput/MessageList/ChatRecite 新建）。

**Q: 还是流式输出,当前架构太差之后可以重构**
→ 确认 SSE 流式方案。

---

## 3. 学习报告

**Q: 下一步聊一下学习报告的生成内容和展示方式**
→ 分析当前报告实现：ReportService.aggregate() 简单统计、DeepSeekLlmGateway.generateReport() AI 报告。问题：aggregate() 被覆盖、没有模块维度拆分、没有历史对比。

**Q: 你说的这些都需要,其次生成前端样例给我挑选一下**
→ 展示三种报告风格：A 仪表盘网格 Dashboard、B 对话流 Conversational、C 紧凑卡片 Compact Card。用户选 A。

**Q: （趋势图）**
A: 三个选项——A 纯 CSS 柱状图 / B 折线图 / C 先不要。选 B（SVG 手写）。

**Q: 你想要的后端方案**
A: A 后端增强统计+LLM 写建议 / B 全塞给 LLM / C 后端全量统计。选 A。
用户补充：LLM 占比多一点,因为评价各个方面不是死板的代码可以解决的。
→ 调整为 Java 45% / LLM 55%。Java 只汇总数字，LLM 做全部判断和文字输出。

**Q: 每一次的评价增添一个隐藏模块,LLM索引,我计划常驻上下文帮助大模型快速汇总**
→ learning_journal 表设计：存储 LLM 摘要 JSON，下次注入最近 5 次到 prompt。

---

## 4. 成就徽章

**Q: 接下来聊聊成就徽章的计划设计**
→ 回顾 V2 文档已有框架（20+ 徽章、5 类、3 个发放时机）。

**Q: （展示风格）**
A: 三种——A 网格陈列 / B 时间线 / C 分类卡片。用户选 A。

**Q: 有类似真正徽章的ui设计吗**
→ 展示三种拟物设计：A 勋章/Medal、B 六边形蜂巢、C 盾牌绶带。用户选 A。

**Q: 获得徽章时的通知方式**
A: A 对话框系统消息+顶部横幅 3 秒 / B 静默获得 / C 全屏弹窗。选 A。

**Q: 点击徽章显示什么**
A: A 详情卡片 / B 翻转动画 / C 不做交互。选 A。

**Q: 徽章数量**
A: A 够了 / B 偏少 / C 太多。选 B（偏少）。

**Q: 往哪个方向加**
A: A 模块细分 / B 隐藏惊喜 / C 以上都要。选 C。

→ 产出完整清单：46 枚（背诵量 5 + 质量 5 + 坚持 5 + 模块单枚 19 + 组合 4 + 趣味隐藏 8），其中 11 枚隐藏。

**Q: 确认** → 确认清单。

---

## 5. 间隔重复

**Q: 聊一聊间隔重复**
→ 回顾 V2 文档方案。三个核心问题需要重新决定：背诵入口变化、startRecite 出题逻辑变化、submitAnswer 后多了更新间隔。

**Q: 这个功能跟现有的"自由背诵"是什么关系**
A: A 替代 / B 新增入口 / C 融合。
用户选 B，并说明核心理念：自由背诵是能力测评（随机出题），模块背诵是学习掌握，今日复习只出背过的题。

**Q: level改为评分0~100,否则可能导致题数过多,取十道题即可,这十道题直接展示答案**
→ mastery_score 0-100 替代枚举。今日复习：10 题固定，不出新题，自评驱动。

**Q: 今日复习,不需要评分,不需要惊喜题**
→ 确认。

---

## 6. 追问链

**Q: 深度追问链聊一聊**
→ 提出 LLM 判断逻辑（shouldContinue 字段）、最多 3 层、每层用户许可。

**Q: 首先追问的问题可能不在题库中,所以默认所有追问的问题都不在题库中,然后追问需要申请并向用户展示方向,获取许可后开始追问**
→ 四个约束：不在题库、需用户许可、仅用于回忆、不介入其他模块。追问附在报告末尾（问题和关键词），不做评分不进 user_progress。

**Q: 再次确定一下,所有背诵,除了追问,必须来自当前现有的数据题库**
→ 硬约束确认。

---

## 7. 基础设施决策

**Q: 我打算像Ragent一样将一些内容放在Mysql里来减轻PSQL的压力**
A: Ragent 不是 MySQL+PG 双库，就用了 PG 一个库。加第二个数据库在这个规模下没有实际收益。

**Q: Ragent没用向量数据库是怎么创建RAG的**
→ 详细分析：Ragent 默认用 PostgreSQL + pgvector 插件，向量存在 `t_knowledge_vector` 表，`<=>` 余弦距离算子 + HNSW 索引。

**Q: 你认为哪个方案更好（pgvector vs Milvus）**
A: pgvector。739 题、几十个用户——Milvus 是开卡车去买菜。部署从 4 容器降到 1（现在 3：PG+Redis+RocketMQ）。

**Q: 那就将Ragent模板的改造方案设计为计划**
→ 确认 pgvector 迁移。

---

## 8. 用户会话隔离

**Q: 用户会话隔离方案聊一下**
→ 分析当前问题：sessionQuestions 单例 Map、无 userId 校验、只存一道题、无过期清理。

**Q: 我没理解你说的问题,每个用户打开的页面不都是一个新的agent吗**
→ 用户纠正：所有用户连同一个后端，不是"多 agent"。真正的问题不是"单例 Map 共享"，而是没有拦截器自动注入用户 + session 该放 DB/Redis 而非内存。

**Q: 借鉴一下Ragent的实现**
→ UserContext (TTL) + UserContextInterceptor + 所有 SQL 带 WHERE user_id=?。三层隔离：线程/数据/会话。

**Q: 用户的上下文是存在哪里的**
A: JVM 线程的 ThreadLocal 里。请求进来→Interceptor 注入→请求结束 clear。TTL 比普通 ThreadLocal 多一个父子线程传递能力。

**Q: Ragent用到sql型数据库了吗**
A: 用了。我的陈述需要修正——Ragent 的会话/消息/摘要全部落 PG 表，每张表都有 user_id 列。

---

## 9. Redis 和 MQ 决策

**Q: Ragent是怎么使用 Redis 和 MQ 的**
→ 详细分析：ChatQueueLimiter 用 Redis Semaphore+Lua 脚本做 SSE 全局并发排队（535 行），IdempotentSubmitAspect 用 Redis 锁防重。RocketMQ 用于文档异步分块（上传→Tika 解析→分块→Embedding→写库）。

**Q: 我认为需要,虽然目前数据不大,但我需要学习这个思路**
→ 确认加入 Redis + MQ。

**Q: 先用redis和MQ的方案框架定下来**
A: Redis：recite:session:{sid} (JSON, TTL 2h) + recite:score:slots (Semaphore 10)。RocketMQ：recite-report-topic → ReportGenerateConsumer。前端轮询 2 秒一次。

**Q: A是不是开销比较大（轮询 vs SSE 挂起）**
A: 最坏 5 秒内最多 3 次请求，每次 <1ms。100 用户同时 60 QPS，轻松扛。SSE 挂起占 Tomcat 线程反而更重。

---

## 10. 可观测性

**Q: Ragent的观测链路是怎么设计的**
→ 三层：注解（@RagTraceRoot/@RagTraceNode）→ AOP 切面采集 → DB 存储（trace_runs + trace_nodes，树形结构带 parentNodeId+depth）。

**Q: 根据你的解释我认为,背诵的链路太粗糙了**
→ 拆细：submitAnswer 链路 9 个节点（AUTH→CACHE→VALIDATE→LLM→DB×2→BUSINESS→CACHE→SSE），finishRecite 链路 8 个节点，MQ 消费者独立 trace。

**Q: 怎么做一个方便的运维后台**
→ 三层方案。层级二（管理后台内嵌）被选中：4 个 API + 1 个 AdminMonitor.vue。详情卡片+环节耗时表+链路可展开。

---

## 11. 架构设计

**Q: 领域划分怎么设计**
→ 五子域：recite/progress/report/achievement/knowledge。recite 是编排中心，其他子域不互相依赖。

**Q: 我需要的是完全重构,而不是在现有代码上修改**
→ 确认完全重写。新项目目录 recite-v2，旧 recite 不动。

**Q: 整个项目实现过程,我需要完成从脚手架开始,完成底层设计,然后一次实现一个功能,每完成一次编码我要push到github**
→ 确认。从零开始逐 Phase push。

**Q: 按照E:\xiangmu\beisong\示例项目\ai-agent-scaffold-lite 完成脚手架底层的构建**
→ 使用 scaffold 模板，创建六模块骨架。

**Q: 先按照Phase根据需要的功能设计计划文件:需要有对外的接口,涉及到的库表,导入哪个模块的类**
→ Phase 计划格式确定。

**Q: 没有我的许可,不允许进行任何代码开发,并且要明确我要开发哪部分**
→ 硬规则：先出文档，审批后编码。

**Q: 先设计数据库,落地文档,再按照Phase设计计划**
→ schema.sql 11 张表完成。

---

## 12. DDD 架构探讨

**Q: 共享内核的作用是什么 / 有实现案例吗**
→ Eric Evans 2003 原著定义。2025 年 dev.to 自定义 BaseAggregateRoot 实现。Spring Data 有 AbstractAggregateRoot 但依赖 JPA。

**Q: 可以实现,写进脚手架然后push,标注为共享内核尝试**
→ 提交了 8 个文件（Entity/AggregateRoot/DomainEvent/ValueObject/DomainException + ReciteSession 示例）。

**Q: 将代码版本恢复到共享内核尝试,只实现port/out,SessionPort+ArchUnit不允许实现**
→ git reset 回退。只保留 port/out 目录（.gitkeep 占位）。

**Q: 砍掉共享内核**
→ 删除。子域间用 MQ 通信，不用 DomainEvent 基类。

**Q: 领域事件机制只是分布式的一种体现对吧,用mq就可以解决**
A: 对。MQ 天然做子域异步通信——三个子域各自消费同一条消息，互不感知。

**Q: port接口位置不明确是什么意思 / 有最近的实现参考案例吗**
→ 六边形架构的 Port/Adapter 模式。2025 年 GitHub 多个项目采用 port/in + port/out。

**Q: 那就在port/out吧**
→ 确认。五子域各建 `port/out/` 目录。

---

## 13. 技术栈确认

| 决策 | 结论 |
|------|------|
| 数据库 | PostgreSQL + pgvector |
| 缓存 | Redis (Redisson) |
| 消息队列 | RocketMQ |
| 认证 | Sa-Token JWT + UserContext(TTL) |
| ORM | MyBatis Plus |
| 前端状态 | Pinia |
| API 文档 | Swagger 3 (springdoc) |
| 测试报告 | Allure |
| 会话存储 | Redis TTL 2h |
| 容器 | PG + Redis + RocketMQ = 3 |

---

## 14. 前端相关

**Q: Pinia是什么聊一聊 / 这些全是前端的内容吗**
A: Vue 3 官方状态管理，全在浏览器跑，不需要后端改动。3 个 Store：recite/achievement/auth。

**Q: 路由呢**
A: 就是 router/index.js，URL→Vue 组件映射。后面聊前端整体设计时细说。

**Q: CI/CD是什么 / 有什么用呢**
A: Git push → 自动编译+测试+部署。单人开发本地就能跑，不是必需品。跳过。

**Q: 测试是怎么实现的 / Ragent有什么可以借鉴的**
A: Ragent 测试不算好（全是 @SpringBootTest，println 当断言）。我们三层：单元/集成/E2E，P0 优先 6 个，Allure 报告。

---

## 15. 错误处理

**Q: Ragent是怎么实现的（错误处理）**
A: AbstractException→ClientException/ServiceException + IErrorCode + @RestControllerAdvice 全局拦截。业务代码直接抛，不 try-catch。

→ 我们项目采用相同模式：AppException 拆 Client/Service 两子类，新增 GlobalExceptionHandler，前端 axios 拦截器统一处理。

---

## 16. 实施状态

**已完成 (Phase 0-2):**
- 脚手架六模块骨架 + POM 依赖
- types (Constants/ResponseCode/ReciteMode/AppException)
- api (Response.java)
- 五子域 port/out 目录约定
- schema.sql (11 张表)
- Docker Compose 设计

**待实施 (Phase 3-11):**
Phase 3 knowledge → 4 auth → 5 recite → 6 progress → 7 report → 8 achievement → 9 可观测性 → 10 前端 → 11 测试

---

## 17. 设计文档索引

| 文档 | 路径 |
|------|------|
| V2 完整设计 spec | `E:\xiangmu\beisong\recite\docs\superpowers\specs\2026-06-11-chat-recite-design.md` |
| 实施总计划 | `recite-v2/docs/superpowers/plans/2026-06-12-v2-implementation-plan.md` |
| 数据库 schema | `recite-v2/docs/dev-ops/sql/schema.sql` |
| 前端样例 | `recite\docs\superpowers\specs\mockup-v2.html` |
| 运维后台样例 | `recite\docs\superpowers\specs\mockup-admin-monitor.html` |
| 上下文汇总 | `recite-v2/docs/CONTEXT.md` |
| 架构图 | `E:\xiangmu\beisong\项目架构.drawio` |
