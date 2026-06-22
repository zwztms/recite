# 翻卡学习 + 个人主页 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan phase-by-phase. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增个人主页（学习概览+双入口）和翻卡学习（侧边索引+自由浏览+标记掌握度），重构导航结构

**Architecture:** 遵循现有 DDD 六模块架构。新增 2 个 Controller、2 个 API 接口、4 个 DTO、3 个前端页面、5 个前端组件。全部复用现有 SPI Port。

**Tech Stack:** Java 17 · Spring Boot 3.4.3 · MyBatis Plus · Vue 3 + Pinia + Tailwind CSS v4

---

## 文件全貌

```
新增后端 (8 文件):
  recite-api/.../IHomeService.java           REST 契约：GET /home/dashboard
  recite-api/.../ILearnService.java          REST 契约：GET /learn/questions + POST /learn/mark
  recite-api/.../dto/HomeDashboardDTO.java   主页聚合响应
  recite-api/.../dto/LearnQuestionDTO.java   学习题目响应
  recite-api/.../dto/MarkRequestDTO.java     标记请求体
  recite-domain/.../home/service/HomeService.java   跨子域聚合编排
  recite-domain/.../learn/service/LearnService.java 题目列表+标记编排
  recite-trigger/.../http/HomeController.java       主页控制器
  recite-trigger/.../http/LearnController.java      学习控制器

新增前端 (5 文件):
  frontend/src/views/HomePage.vue                个人主页
  frontend/src/views/CardLearn.vue               翻卡学习页
  frontend/src/components/home/ReportModal.vue   背诵报告弹窗
  frontend/src/components/learn/SideIndex.vue    侧边索引
  frontend/src/components/learn/QuestionCard.vue 题目卡片

修改 (4 文件):
  frontend/src/api/index.js          新增 3 个 API 函数
  frontend/src/router/index.js       新增 /home /learn 路由，/ 重定向改为 /home
  frontend/src/App.vue               导航链接加入"首页""学习"
  frontend/src/stores/authStore.js   登录后跳转 /home
```

---

## Phase 依赖链

```
Phase 12 (主页后端) ──→ Phase 14 (主页前端+路由)
                            │
Phase 13 (学习后端) ──→ Phase 15 (学习前端)
                            │
                     Phase 16 (导航收尾，依赖 14+15 页面存在)
```

12 和 13 可并行（互不依赖后端），14 和 15 可并行（互不依赖前端）。

---

## Phase 12：个人主页后端 API

### 主题

新增 `GET /home/dashboard` 聚合 API，一次返回主页全部数据。

### 目的

前端只需一次请求即可渲染个人主页所有区域——问候统计、模块掌握度、7天趋势、徽章、薄弱标签、AI建议、最近背诵列表。

### 影响

| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无（全部复用已有 Port） |
| 修改旧代码 | 无（新增 Controller+Service+DTO，不碰现有文件） |
| 前端 | 占位 HomePage.vue + api/index.js 追加函数 + router 预留 /home |

### 子任务

- [ ] **1. HomeDashboardDTO** — `recite-api/.../dto/HomeDashboardDTO.java`
  - 顶层字段：user, stats, moduleMastery, trend, badges, weakTags, advice, recentRecites
  - 内部静态类：UserInfo, Stats, ModuleMastery, TrendBar, BadgeItem, RecentRecite
  - 编译：`mvn compile -pl recite-api -q`

- [ ] **2. IHomeService** — `recite-api/.../IHomeService.java`
  - 接口：`@GetMapping("/dashboard") Response<HomeDashboardDTO> dashboard()`
  - 编译：`mvn compile -pl recite-api -q`

- [ ] **3. HomeService** — `recite-domain/.../home/service/HomeService.java`
  - 注入 7 个已有 Port：ReciteRecordPort, ProgressPort, StreakPort, AchievementPort, ReportPort, QuestionPort, ModulePort
  - 方法：`HomeDashboardDTO build(Long userId)` — 逐一调用 Port 组装各区域数据
  - `buildStats`: streakPort + reciteRecordPort.countByUserId + progressPort.countMastered
  - `buildModuleMastery`: modulePort.listAll + questionPort.countByModule + progressPort.findByUserId 内存过滤 masteryScore>=80
  - `buildTrend`: reciteRecordPort.findByUserId(500) → 内存按 LocalDate 分组 → 近 7 天柱状数据
  - `buildBadges`: achievementPort.findEarnedBadgeMap → 按时间倒序取前 3 枚
  - `buildWeakTags`: reportPort.findRecentJournals(5) → Gson 解析 summaryJson 提取 weakTags
  - `buildAdvice`: reportPort.findRecentJournals(1) → 取最新 advice
  - `buildRecentRecites`: reciteRecordPort.findByUserId(200) → 按 sessionId 分组 → 取最近 5 场
  - 编译：`mvn compile -pl recite-domain -q`

- [ ] **4. HomeController** — `recite-trigger/.../http/HomeController.java`
  - 实现 IHomeService，注入 HomeService
  - `dashboard()`: UserContext.getUserId() → homeService.build(userId) → Response.ok(dto)
  - 编译：`mvn compile -pl recite-trigger -q`

- [ ] **5. 前端预埋** — 3 处小改动
  - `api/index.js`: 追加 `getHomeDashboard()` 函数
  - `router/index.js`: 追加 `/home` 路由 + `/` 重定向改为 `/home`
  - 创建占位 `HomePage.vue`（Phase 14 替换为完整版）
  - 验证：`cd frontend && npx vite build`

- [ ] **6. 全量编译 + 提交**
  ```bash
  mvn install -DskipTests -q
  git add -A
  git commit -m "phase12: 个人主页后端 — GET /home/dashboard 聚合 API"
  ```

---

## Phase 13：翻卡学习后端 API

### 主题

新增 `GET /learn/questions` 和 `POST /learn/mark`，支持题目列表浏览和掌握度标记。

### 目的

前端翻卡学习页面通过这两个 API 获取题目列表（含已掌握/未掌握状态）和提交标记。全部复用已有 QuestionPort、ProgressPort、SpacedRepetitionService。

### 影响

| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无 |
| 修改旧代码 | 无 |
| 前端 | api/index.js 追加 2 个函数 |

### 子任务

- [ ] **1. DTO** — `LearnQuestionDTO.java` + `MarkRequestDTO.java`
  - LearnQuestionDTO: id, question, content(答案), moduleKey, moduleName, category, tags, difficulty, mastered(boolean)
  - MarkRequestDTO: questionId, mastered(boolean)
  - 编译：`mvn compile -pl recite-api -q`

- [ ] **2. ILearnService** — `recite-api/.../ILearnService.java`
  - `GET /learn/questions?moduleKey&order=seq|random&filter=all|unmastered|mastered`
  - `POST /learn/mark` body: MarkRequestDTO
  - 编译：`mvn compile -pl recite-api -q`

- [ ] **3. LearnService** — `recite-domain/.../learn/service/LearnService.java`
  - 注入 4 个已有依赖：QuestionPort, ProgressPort, ModulePort, SpacedRepetitionService
  - `getQuestions(userId, moduleKey, order, filter)`: 按 order 调 searchByModule(searchRandom) 拉题 → 查 user_progress 标记 mastered → 模块名称映射 → 按 filter 筛选 → 返回 List<LearnQuestionDTO>
  - `mark(userId, questionId, mastered)`: 查题 → mastered?8:3 分值 → spacedRepetitionService.calculateAfterScore → progressPort.save/update
  - 编译：`mvn compile -pl recite-domain -q`

- [ ] **4. LearnController** — `recite-trigger/.../http/LearnController.java`
  - 实现 ILearnService，注入 LearnService
  - `questions()`: UserContext.getUserId() → learnService.getQuestions() → Response.ok(list)
  - `mark()`: learnService.mark(userId, dto.questionId, dto.mastered) → Response.ok()
  - 编译：`mvn compile -pl recite-trigger -q`

- [ ] **5. 前端预埋** — `api/index.js` 追加 `getLearnQuestions()` 和 `markMastery()` 函数

- [ ] **6. 全量编译 + 提交**
  ```bash
  mvn install -DskipTests -q
  git add -A
  git commit -m "phase13: 翻卡学习后端 — GET /learn/questions + POST /learn/mark"
  ```

---

## Phase 14：个人主页前端

### 主题

实现 HomePage.vue（完整个人主页）和 ReportModal.vue（背诵报告弹窗）。

### 目的

用户登录后看到学习概览，可一目了然当前进度、快速进入翻卡学习或对话背诵、查看最近背诵记录并点击查看详细报告。

### 影响

| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无 |
| 修改旧代码 | router/index.js（/home 路由已预埋，替换占位组件） |
| 依赖 | Phase 12（后端 API 就绪） |

### 子任务

- [ ] **1. ReportModal.vue** — `frontend/src/components/home/ReportModal.vue`
  - Props: visible(Boolean), sessionId(String)
  - Emits: close
  - 打开时调 `getReport(sessionId)` 获取报告数据
  - 展示：总分/均分/题数三卡、优势标签、薄弱标签、AI 评语
  - Teleport 到 body，遮罩层点击关闭

- [ ] **2. HomePage.vue** — `frontend/src/views/HomePage.vue`（覆盖占位文件）
  - onMounted 调 `getHomeDashboard()` 获取数据
  - 四大区域：问候+四维统计 → 双 CTA → 三列卡片（掌握/趋势/徽章+标签+建议）→ 最近背诵（点击弹出 ReportModal）
  - 模块掌握度显示"已掌握题数/总题数"格式
  - 双 CTA 分别跳转 `/learn` 和 `/recite`

- [ ] **3. 构建验证 + 提交**
  ```bash
  cd frontend && npx vite build
  git add -A
  git commit -m "phase14: 个人主页前端 — HomePage + ReportModal"
  ```

---

## Phase 15：翻卡学习前端

### 主题

实现 CardLearn.vue + SideIndex.vue + QuestionCard.vue，侧边索引联动内容区浏览。

### 目的

用户可以自由浏览题目、点击展开答案、标记掌握度。侧边索引快速跳转，滚动联动高亮。

### 影响

| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无 |
| 修改旧代码 | 无 |
| 依赖 | Phase 13（后端 API 就绪） |

### 子任务

- [ ] **1. SideIndex.vue** — `frontend/src/components/learn/SideIndex.vue`
  - Props: questions(Array), activeId(String)
  - Emits: select(questionId)
  - 列出全部题目编号+标题+掌握状态圆点(绿/灰)
  - 当前选中项橙色高亮，点击 emit select 跳转

- [ ] **2. QuestionCard.vue** — `frontend/src/components/learn/QuestionCard.vue`
  - Props: question(Object), index(Number)
  - Emits: mark(questionId, mastered)
  - 点击展开/收起答案（手风琴，内部 expanded ref）
  - 展开后显示答案全文 + 标记按钮（已掌握/标记未掌握）
  - 编号圆圈：绿色=已掌握，灰色=未掌握

- [ ] **3. CardLearn.vue** — `frontend/src/views/CardLearn.vue`
  - 控制栏：模块下拉（从 listModules API 获取 ONLINE 模块）、顺序/乱序切换、全部/未掌握/已掌握筛选
  - 主体：左侧 SideIndex + 右侧 QuestionCard 列表
  - 滚动内容区时自动更新 activeId（联动索引高亮）
  - 点击索引项 → 右侧滚动到对应题目
  - 标记后即时更新本地 mastered 状态

- [ ] **4. 构建验证 + 提交**
  ```bash
  cd frontend && npx vite build
  git add -A
  git commit -m "phase15: 翻卡学习前端 — CardLearn + SideIndex + QuestionCard"
  ```

---

## Phase 16：导航收尾

### 主题

更新顶部导航、路由和登录跳转，串联全部页面。

### 目的

用户登录后进入个人主页，导航栏可访问全部功能。这是最小的改动——仅调整已有文件的链接和跳转路径。

### 影响

| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 修改旧代码 | App.vue（导航链接）、router/index.js（/learn 路由）、authStore.js（登录跳转路径） |
| 影响现有功能 | 背诵、成就、管理后台路由不变，仅新增入口 |
| 依赖 | Phase 14+15（HomePage 和 CardLearn 组件存在） |

### 子任务

- [ ] **1. App.vue** — 导航链接调整
  - 加入"首页"(`/home`)、"学习"(`/learn`)
  - 保留"背诵"(`/recite`)、"成就"(`/achievements`)、"管理"(admin)
  - 顺序：首页 | 学习 | 背诵 | 成就 | 管理

- [ ] **2. router/index.js** — `/learn` 路由
  - 追加 `{ path: '/learn', component: () => import('../views/CardLearn.vue'), meta: { auth: true } }`

- [ ] **3. authStore.js** — 登录后跳转 `/home`
  - `login()`、`register()`、`adminLogin()` 中的 `router.push('/recite')` 改为 `router.push('/home')`（共 3 处）

- [ ] **4. 构建验证 + 提交**
  ```bash
  cd frontend && npx vite build 2>&1 | tail -3
  git add -A
  git commit -m "phase16: 导航收尾 — 首页+学习入口 + 登录跳转 /home"
  ```

---

## 汇总

| Phase | 主题 | 目的 | 新文件 | 改文件 | 新表 | 新 SPI |
|:--:|------|------|:--:|:--:|:--:|:--:|
| 12 | 个人主页后端 | 聚合 API，一次返回主页全部数据 | 4 Java | 3 JS(预埋) | 无 | 无 |
| 13 | 翻卡学习后端 | 题目列表 + 掌握度标记 API | 4 Java | 1 JS | 无 | 无 |
| 14 | 个人主页前端 | 学习概览首页 + 报告弹窗 | 2 Vue | 1 Vue(替换占位) | 无 | 无 |
| 15 | 翻卡学习前端 | 侧边索引 + 题目浏览 + 标记 | 3 Vue | — | 无 | 无 |
| 16 | 导航收尾 | 串联全部页面入口 | — | 3 JS/Vue | 无 | 无 |
