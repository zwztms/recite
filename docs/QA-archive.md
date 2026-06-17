# V2 设计讨论 Q&A 原文归档

> 按主题分类，保留关键问答原文。用于回顾决策理由。

---

## 一、架构与重构策略

**Q: 当前的后端是不是需要完全重构,在之前的代码上修改太重了**
A: 不是完全重写，但核心文件改动量确实很大。逐个模块看——types 小改、api 中改、domain 核心重写、infrastructure 中改、trigger 中改、app 不动。

**Q: 领域划分怎么设计**
A: 按业务能力拆五个子域：recite（背诵核心）、progress（进度）、report（报告）、achievement（成就）、knowledge（知识库）。子域间通过 SPI 接口调用，recite 是编排中心。

**Q: 我需要的是完全重构,而不是在现有代码上修改**
A: 架构不变的部分只有 DDD 六模块目录结构、技术栈、数据库表。其他全部重写。完全不留的：ReciteLogicService、MilvusQuestionProvider、CategoryRecite.vue、RandomRecite.vue。

**Q: 当前设定为重构吧**
→ 确认。新项目目录 `E:\xiangmu\beisong\recite-v2\`，旧 `recite\` 不动。

**Q: 整个项目实现过程,我需要完成从脚手架开始,完成底层设计,然后一次实现一个功能,每完成一次编码我要push到github**
A: 从零开始——脚手架 → types → infrastructure → domain → trigger → frontend。每 Phase 独立 push。

---

## 二、DDD 架构设计

**Q: 共享内核的作用是什么**
A: 五个子域各自有聚合根、实体、值对象。没有共享内核的话每个子域自己发明一套规则。共享内核就是公共约定的核心概念。不放工具函数，只放领域层共用基类。

**Q: 共享内核在DDD架构的身份是什么**
A: DDD 原著 Eric Evans 定义的 Shared Kernel 模式。多个限界上下文之间有意共享的那部分领域模型。

**Q: 就是类似AOP的实现对吧**
A: 不是。AOP 是横向拦截，共享内核是纵向继承。聚合根收集事件，框架在事务提交时自动发出——这块确实像 AOP，但不是共享内核本身。

**Q: 有共享内核的实现案例吗**
A: 2025 年 dev.to 上的自定义 BaseAggregateRoot 实现，纯 Java 零依赖，适配 MyBatis 场景。Spring Data 有官方的 AbstractAggregateRoot，但依赖 JPA。

**Q: 这套实现是在哪一年搜索到的**
A: 2025-2026 年的资料。dev.to/paszekdev 2025 年 11 月，baeldung 2025 年，Spring Modulith 持续更新。

**Q: port接口位置不明确是什么意思**
A: SPI 接口放哪没有约定——service/、repository/、port/out/ 三个地方都可能。Port 模式约定领域层只定义接口，基础设施层实现。

**Q: 这个也是DDD原始的定义吗,有最近的实现参考案例吗**
A: Port/Adapter 来自 Alistair Cockburn 的六边形架构（2005），不是 DDD 原著。今天两个几乎总是一起用。2025 年 GitHub 上多个项目（lefranchi、davidgfolch、rajkundalia）都采用 port/in + port/out 模式。

**Q: 有什么高分案例吗,随便一个不知名项目使用的话没必要说**
A: 实话实说——Port/Adapter 命名是学术概念，实际项目中接口直接叫 XxxRepository 或 XxxGateway 更常见。关键不是叫什么名字，是接口放领域层、实现放基础设施层这个原则。

**Q: 那就在port/out吧**
→ 确认。每个子域下建 `port/out/` 包。

---

## 三、共享内核的砍掉

**Q: 可以实现,写进脚手架然后push,标注为,共享内核尝试**
→ 写入并提交。包含 Entity/AggregateRoot/DomainEvent/ValueObject 基类 + ReciteSession 聚合示例。

**Q: 将代码版本恢复到共享内核尝试,只实现port/out,SessionPort示例 + ArchUnit架构守护不允许实现**
→ git reset 回退。只留 port/out 目录（.gitkeep 占位），不要 SessionPort.java，不要 ArchUnit。

**Q: 砍掉共享内核**
→ 删除 shared/ 目录，删除 ReciteSession 等示例。子域间直接用 MQ 通信。

**Q: 领域事件机制只是分布式的一种体现对吧,用mq就可以解决**
A: 对。领域事件本质就是子域之间的异步通信。MQ 天然解决——不需要 DomainEvent 接口，不需要 registerEvent/pullEvents，不需要 Event Listener。三个子域各自消费同一条消息，互不感知。

---

## 四、技术选型

**Q: Ragent用到sql型数据库了吗**
A: 用了。Ragent 用 PostgreSQL + pgvector。会话、消息、摘要全部落 SQL 表，每张表都有 user_id 列。

**Q: 我打算像Ragent一样将一些内容放在Mysql里来减轻PSQL的压力**
A: Ragent 不是 MySQL + PG 双库，就用了 PG 一个库。加第二个数据库在这个规模下没有实际收益。

**Q: Ragent没用向量数据库是怎么创建RAG的**
A: Ragent 默认用 PostgreSQL + pgvector 插件，向量存在 `t_knowledge_vector` 表里，用 `<=>` 余弦距离算子 + HNSW 索引做 ANN 搜索。

**Q: 你认为哪个方案更好（pgvector vs Milvus）**
A: pgvector。739 道题、几十个用户——Milvus 是开卡车去买菜。一道 SQL 就能做的事现在要维护 4 个容器。到 100 万向量之前 pgvector 都不会是瓶颈。

**Q: Ragent是怎么使用（Redis和MQ）的**
A: Redis 做 SSE 全局并发排队（Redisson Semaphore + Lua 脚本）+ 防重复提交。RocketMQ 做文档异步分块（Tika 解析+Embedding+写库）。两个都是多租户场景才需要，单人低频用不上。

**Q: 我觉得需要（Redis和MQ）,虽然目前数据不大,但我需要学习这个思路**
→ 确认。Redis 做会话缓存 + 评分并发信号量。RocketMQ 做报告异步生成。

**Q: 先用redis和MQ的方案框架定下来**
A: Redis: recite:session:{sid} (JSON, TTL 2h) + recite:score:slots (Semaphore 10)。RocketMQ: recite-report-topic → ReportGenerateConsumer。前端轮询 GET /{sid}/report，每 2 秒一次。

**Q: A是不是开销比较大（轮询 vs SSE 挂起）**
A: 最坏情况 5 秒内最多 3 次请求，每次 <1ms 数据库时间。100 个用户同时 60 QPS，Spring Boot 轻松扛。B（SSE 挂起）反而更重——占 Tomcat 线程什么都不干。

**Q: 可以（轮询）**
→ 确认。

---

## 五、会话隔离

**Q: 用户的上下文是存在哪里的**
A: JVM 线程的 ThreadLocal 里。请求进来 → Interceptor 注入 → 请求结束 clear。用 TTL（TransmittableThreadLocal）而不是普通 ThreadLocal，父线程上下文可传递给子线程。

**Q: 我觉得需要借鉴Ragent的实现**
A: Ragent 三层隔离：线程级（TTL + Interceptor）、数据级（SQL WHERE user_id=?）、会话级（会话存 DB 表）。我们当前做法本质一样——Controller 里手动 getLoginIdAsLong()，只是差一层封装。

**Q: 我没理解你说的问题,每个用户打开的页面不都是一个新的agent吗**
A: 用户纠正——所有用户连的是同一个后端。存储是服务端的 DB/Redis，不是"多 agent 多上下文窗口"。sessionQuestions Map 是服务端单例，天然共享。问题不是"多 agent"，是内存 Map 没有 userId 校验。

---

## 六、可观测性

**Q: Ragent的观测链路是怎么设计的**
A: 三层：注解系统（@RagTraceRoot + @RagTraceNode）→ AOP 切面采集 → DB 表存储（trace_runs + trace_nodes，树形结构带 parentNodeId + depth）。节点类型有 LLM_PROVIDER、REWRITE、RETRIEVE 等。

**Q: 根据你的解释我认为,背诵的链路太粗糙了**
A: 拆细——submitAnswer 含 AUTH→CACHE→VALIDATE→LLM→DB×2→BUSINESS→SSE。finishRecite 含 CACHE→DB×3→BUSINESS→MQ→CACHE。MQ 消费者侧另开一条独立 trace。

**Q: 怎么做一个方便的运维后台**
A: 三个层次。层级一：命令行应急（SQL 查 trace 表）。层级二：管理后台内嵌页面（推荐，3 个新 API + 1 个 Vue 页面）。层级三：Grafana+Prometheus（过重，不建议）。

**Q: 二（管理后台内嵌）**
→ 确认。AdminMonitor.vue + AdminController 4 个 API。

---

## 七、前端相关

**Q: Pinia是什么聊一聊**
A: Vue 3 官方的状态管理库，Vuex 的接班人。替代组件 data() 大对象。需要 3 个 Store：recite（会话/消息/SSE）、achievement（徽章/toast）、auth（token/role）。

**Q: 这些全是前端的内容吗**
A: 对，全在浏览器里跑，跟 Java 后端没关系。

**Q: 路由呢**
A: 就是 `router/index.js`，URL 和 Vue 组件的映射。后面聊前端整体设计时再细说。

---

## 八、错误处理

**Q: Ragent是怎么实现的（错误处理）**
A: 三层：异常分类（AbstractException→ClientException/ServiceException）→ 错误码接口（IErrorCode）→ @RestControllerAdvice 全局拦截。业务代码直接抛，不需要 try-catch+return Response.fail()。

---

## 九、测试

**Q: 测试是怎么实现的,Ragent有什么可以借鉴的吗**
A: Ragent 的测试不算好——全是 @SpringBootTest，println 当断言。但框架对。我们项目三层：单元（JUnit+AssertJ）、集成（MockMvc）、E2E（TestRestTemplate）。P0 优先 6 个。

**Q: 但测试结果我需要用页面给我展示会不会不好实现**
A: Allure 框架，零开发。mvn test → allure generate → HTML 报告自动生成。

---

## 十、CI/CD

**Q: CI/CD是什么**
A: 三句话——CI: 每次 push 自动编译+跑测试。CD: 测试通过自动打包部署。对你来说就是 Git push → GitHub Actions 自动跑 mvn compile + test → 告诉你结果。

**Q: 有什么用呢**
A: 替你跑 `mvn compile` 和 `mvn test`，push 完不用手动跑。但单人开发本地就能跑，不是必需品。

**Q: 没用,我一般都在本地测试**
→ 跳过。不加 CI/CD。

---

## 十一、实施策略

**Q: 先按照Phase根据需要的功能设计计划文件:需要有对外的接口,涉及到的库表,导入哪个模块的类**
→ Phase 计划必须写清：对外接口、涉及表、依赖模块。

**Q: 没有我的许可,不允许进行任何代码开发,并且要明确我要开发哪部分**
→ 只出文档，不写代码。用户审批后动手。

**Q: 先设计数据库,落地文档,再按照Phase设计计划**
→ 先写 schema.sql（11 张表），再逐 Phase 出计划文档。

---

## 十二、之前搜索的关键资料

- 2026 聊天 UI 趋势：去气泡、去头像、暖灰底、橘色点缀
- Ragent 源码分析：UserContext(TTL) + Interceptor、ChatQueueLimiter(Redis Semaphore)、KnowledgeDocumentChunkConsumer(RocketMQ)
- 2025 GitHub 项目：lefranchi/hexagonal-architecture-demo、davidgfolch/hexagonal-springboot
- DDD Shared Kernel: Eric Evans 2003 原著 + 2025 dev.to 实现案例
- 数据库：pgvector HNSW 索引 (M=48, efConstruction=200, COSINE)
