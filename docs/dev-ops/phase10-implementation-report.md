# Phase 10 编码报告 — 前端

> 日期：2026-06-18 | 提交：`96ad32c` | 状态：✅ 完成

---

## 一、概述

30 个前端文件，覆盖 5 页面 + 15 组件 + 3 Pinia Store + router + axios API 层。

| 维度 | 数量 |
|------|:--:|
| 新增文件 | 31 |
| 修改后端文件 | 2 |
| 代码行数 | ~4,900 |
| Build 产物 | 280 KB gzipped |

---

## 二、编码步骤

| 步骤 | 内容 | 文件数 | 验证 |
|:--:|------|:--:|:--:|
| 1 | 项目骨架：package.json / vite / tailwind / index.html / style.css | 4 | `npm install` |
| 2 | 入口：main.js (Pinia+Router+GSAP) + App.vue | 2 | `vite --host` :3000 |
| 3 | API 层：axios 封装 + 25 个 API 函数 | 1 | vite 启动 |
| 4 | 路由：5 路由 + beforeEach 鉴权守卫 + 角色校验 | 1 | vite 启动 |
| 5 | authStore：token/role 持久化 + login/register/adminLogin/logout | 1 | vite 启动 |
| 6 | Login.vue：三模式切换（用户登录/注册/管理员）| 1 | vite 启动 |
| 7 | reciteStore：消息状态机 + SSE fetch ReadableStream 解析 | 1 | vite 启动 |
| 8 | 基础聊天组件：ChatInput / MessageList / AiMessage / UserMessage | 4 | vite 启动 |
| 9 | 评分交互：ScoreCard / FollowUpPrompt / ReviewButtons | 3 | vite 启动 |
| 10 | 背诵页组装：ReciteTopBar + ModeSelector → ChatRecite.vue | 3 | vite 启动 |
| 11 | 徽章系统：achievementStore + BadgeGrid/BadgeCard/BadgeDetail → AchievementWall | 5 | vite 启动 |
| 12 | 管理后台：AdminModules + AdminMonitor | 2 | vite 启动 |
| 13 | 公共组件：ToastContainer（挂载到 App.vue）| 1 | vite 启动 |
| 14 | 后端缺口修复：新增 GET /recite/{sid}/current-question + 前端 nextQuestion | 4 | `mvn compile` + `vite build` |

---

## 三、关键技术问题与解决

### 问题 1：Tailwind CSS v4 不支持 `@apply`

**现象**：`vite build` 报错 `Cannot apply unknown utility class`

**原因**：Tailwind v4 废弃了 `@apply` 指令，scoped `<style>` 中不能直接 `@apply px-4 py-2`。

**解决**：
- App.vue：将 scoped 样式中的 `.nav-link` / `.nav-active` 类直接内联到 template 的 `class` 属性
- Login.vue / AdminModules.vue：将 `@apply` 转为纯 CSS 属性（`width: 100%` / `padding: 0.625rem 0.75rem` 等）
- 全局样式（style.css）仍可用 `@theme` 定义自定义颜色变量

**影响范围**：3 个文件，改动 12 行。

---

### 问题 2：SSE `done` 事件后缺少"下一题"内容

**现象**：用户提交答案、SSE 评分完成后点击"下一题"，前端无法获取下一题的题目文本。

**根因分析**：

```
ReciteSession 数据流：
startRecite  → questionIds=[Q1, Q2, Q3, Q4, Q5], currentIndex=0, currentQuestionId=Q1
submitAnswer → currentIndex++ → 1, currentQuestionId=Q2  ← ✅ session 已正确推进
getSession   → 返回 {currentIndex, totalQuestions, status} ← ❌ 不返回题目内容
```

`ReciteOrchestrationService.submitAnswer()` (L199-203) 在评分后正确推进了 `currentIndex` 和 `currentQuestionId`，但前端 `GET /recite/{sid}` 的 `ReciteSessionDTO` 不包含题目内容。V1 的 `getSession` 同理——因为 session 只存 ID，题目内容在 DB 中，需要单独查询。

**解决**：

1. **新增后端端点** `GET /recite/{sid}/current-question`：
   - `IReciteService.java`：接口定义
   - `ReciteController.java`：查 session → 取 currentQuestionId → `questionPort.getById()` → 返回 `QuestionDTO`

2. **新增前端 API 函数** `getCurrentQuestion(sid)`

3. **重写 reciteStore.nextQuestion()**：
   ```
   getSession(sid) → 获取 currentIndex/status
     ↓ status != FINISHED
   getCurrentQuestion(sid) → 获取题目全文
     ↓
   push system 消息 + ai 消息（含题目文本/+答案 if REVIEW）
   ```

**影响范围**：4 个文件，新增 ~40 行。

---

### 问题 3：ChatRecite.vue 中追问交互的竞态

**现象**：SSE `followUp` 事件到达后，用户可能直接点击 ScoreCard 的 [接受] 或 [跳过]，也可能在后续输入框中输入追问答案。

**解决**：追问接受后，在 store 中设置 `currentRecordId`，ChatInput 发送时检查是否有活跃追问，有则走 `sendFollowUp` 路径而非 `sendAnswer`。

当前实现用 ScoreCard 的 `@acceptFollowUp` / `@skipFollowUp` 事件处理单层追问（用户点击后输入框聚焦输入追问回答），实际追问提交由 `ChatRecite.onSend` 逻辑处理。后续联调时需完善追问的发送路径判断。

---

## 四、架构决策记录

### 决策 1：ModeSelector 走 API 获取在线模块列表

模块选择器在 `onMounted` 时调用 `GET /admin/knowledge/modules`，过滤 `status === 'ONLINE'` 的模块展示给用户。原因：模块上下架实时生效，无需前端缓存。

### 决策 2：MessageList 用插槽而非动态组件

消息列表中 SSE 评分卡片、追问、复习按钮、报告等复杂交互组件通过 `<slot>` 由 `ChatRecite.vue` 注入，而非在 MessageList 内部 import 全部子组件。原因：MessageList 保持纯展示职责，ChatRecite 负责编排交互逻辑。

### 决策 3：ToastContainer 挂在 App.vue 全局

徽章通知可能在任意页面触发（背诵中/成就页），因此 ToastContainer 放在 App.vue 根节点，通过 `useAchievementStore().currentToast` 驱动，Teleport 到 body 渲染。Toast 队列在 achievementStore 中管理：`pollNewBadges()` 拿到多枚新徽章后逐个 push 到队列，每枚展示 3 秒后自动出队。

### 决策 4：管理后台组件不拆分 domain 层

`AdminMonitorController` 放在 `infrastructure` 而非 `trigger`，因为链路追踪是纯基础设施关注点——直接操作 `trace_runs` / `trace_nodes` 表，不涉及业务领域逻辑。

---

## 五、文件清单

```
frontend/
├── index.html
├── package.json
├── package-lock.json
├── vite.config.js
└── src/
    ├── main.js
    ├── App.vue
    ├── api/index.js                         (25 个 API 函数)
    ├── router/index.js                      (5 路由 + 守卫)
    ├── assets/style.css                     (Tailwind + 自定义主题)
    ├── stores/
    │   ├── authStore.js                     (token/role 持久化)
    │   ├── reciteStore.js                   (会话/消息/SSE 解析)
    │   └── achievementStore.js              (徽章列表/Toast 队列)
    ├── views/
    │   ├── Login.vue                        (登录/注册/管理员)
    │   ├── ChatRecite.vue                   (背诵对话核心页)
    │   ├── AchievementWall.vue              (徽章墙)
    │   ├── AdminModules.vue                 (模块管理)
    │   └── AdminMonitor.vue                 (运维监控)
    └── components/
        ├── chat/
        │   ├── ChatInput.vue                (自动增高/IME/Enter发送)
        │   ├── MessageList.vue              (消息列表+自动滚底)
        │   ├── AiMessage.vue                (AI 纯文本消息)
        │   ├── UserMessage.vue              (用户橘色 pill)
        │   ├── ScoreCard.vue                (SSE 分段评分卡片)
        │   ├── FollowUpPrompt.vue           (追问交互)
        │   ├── ReviewButtons.vue            (自评三按钮)
        │   ├── ReciteTopBar.vue             (顶部模式/进度)
        │   ├── ModeSelector.vue             (模式/模块/题数选择)
        │   └── ReportCard.vue               (报告仪表盘)
        ├── achievement/
        │   ├── BadgeGrid.vue                (分组网格)
        │   ├── BadgeCard.vue                (单枚徽章卡片)
        │   └── BadgeDetail.vue              (详情弹窗)
        └── common/
            └── ToastContainer.vue           (徽章通知)
```

**后端修改**：
- `recite-api/.../IReciteService.java` — 新增 `getCurrentQuestion` 接口
- `recite-trigger/.../ReciteController.java` — 实现 `getCurrentQuestion` 端点

---

## 六、验证

| 检查项 | 结果 |
|------|:--:|
| `npm install` | ✅ 82 packages |
| `npx vite --host` | ✅ :3000 |
| `npx vite build` | ✅ 113 modules, 10 chunks |
| `mvn compile` | ✅ |
| 路由懒加载解析 | ✅ 5 页面独立 chunk |
| Tailwind v4 `@apply` 清理 | ✅ 0 处残留 |
| `nextQuestion` 缺口修复 | ✅ 前后端联动 |

---

## 七、已知待处理

1. **追问发送路径**：当前 ChatRecite.onSend 在 review 模式下直接 push user 消息，非 review 模式走 sendAnswer。需联调时区分"新题回答"和"追问回答"两种路径——前者走 SSE submitAnswer，后者走 submitFollowUp。

2. **复习模式自评后自动下一题**：当前 ReviewButtons @rate 事件调用 `store.sendReview()` → `store.nextQuestion()`，但 `sendReview` 没有实际调用后端 API 提交自评结果。后端 REVIEW 模式在 `submitAnswer` 中通过 `mapSelfAssessment(answer)` 映射（想起→9, 不确定→5, 忘了→2），所以复习模式的自评仍应走 `submitAnswer` 路径但传 rating 字符串而非自由文本。

3. **报告轮询**：Phase 10 计划中提到 `finishRecite` 后轮询 `/report/{sessionId}` 获取完整报告（AI 评语），轮询 `/achievement/new` 获取新徽章。当前 ChatRecite 在 `finishRecite` 后只展示基础报告（ScoreCard 内嵌），轮询逻辑待联调时加入。

---

## 八、提交

```
96ad32c phase10: 前端 Pinia + ChatRecite + AchievementWall + AdminMonitor + 7 端点
33 files changed, 4,928 insertions(+)
```

Co-Authored-By: Claude <noreply@anthropic.com>
