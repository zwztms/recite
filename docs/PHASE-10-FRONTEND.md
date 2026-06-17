# Phase 10 — 前端 实施计划

> 日期：2026-06-16 | 状态：计划已审批，待编码

---

## 一、技术栈

| 组件 | 选型 |
|---|---|
| 框架 | Vue 3 (Composition API, `<script setup>`) |
| 构建 | Vite |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| 样式 | Tailwind CSS v4 |
| HTTP | axios |
| SSE | 原生 EventSource |
| 动画 | GSAP (已有) |

---

## 二、要实现哪些功能，怎么实现

### 功能 1：登录/注册页

**页面**：`/login`

**功能**：
- 手机号+密码登录（支持用户名）
- 手机号+密码+昵称注册
- 管理员切换入口（用户名+密码登录）
- 登录成功 → 存 token 到 localStorage → 跳转 `/recite`

**组件**：
```
Login.vue
  ├─ LoginForm (手机号/用户名 + 密码 + 登录按钮)
  ├─ RegisterForm (手机号 + 密码 + 昵称 + 注册按钮)
  └─ AdminLoginToggle (切换管理员登录)
```

---

### 功能 2：背诵对话页（核心）

**页面**：`/recite`

**功能**：合并旧版 CategoryRecite + RandomRecite + Review 三种模式到一个聊天式界面。

**页面布局**：
```
┌─────────────────────────────────────────────┐
│  顶部栏: 模式切换 (模块/随机/复习) + 结束按钮  │
├─────────────────────────────────────────────┤
│                                             │
│  消息列表 (MessageList)                      │
│  ┌─────────────────────────────────────┐    │
│  │ AI: "准备好了吗？开始第 1 题..."      │    │
│  │                                     │    │
│  │      ┌──────────────┐               │    │
│  │      │ 用户回答文字   │  (橘色 pill)   │    │
│  │      └──────────────┘               │    │
│  │                                     │    │
│  │ AI: 分数 8/10                        │    │
│  │     正确点: A, B                     │    │
│  │     遗漏: C                          │    │
│  │     建议: ...                        │    │
│  │     追问: "请深入解释..."             │    │
│  │        [接受] [跳过]                 │    │
│  │                                     │    │
│  │ 复习模式: [✓ 想起] [? 不确定] [✗ 忘了]│    │
│  └─────────────────────────────────────┘    │
│                                             │
├─────────────────────────────────────────────┤
│  输入区 (ChatInput)                          │
│  ┌──────────────────────────────┬─────┐    │
│  │ 自动增高 textarea            │ 发送 │    │
│  │ Enter 发送, Shift+Enter 换行  │     │    │
│  └──────────────────────────────┴─────┘    │
│  暖灰底色 + 橘珊瑚 #f97316 发送按钮          │
└─────────────────────────────────────────────┘
```

**组件树**：
```
ChatRecite.vue
  ├─ ReciteTopBar.vue          模式切换 (tabs) + 题数选择 + 结束按钮
  ├─ MessageList.vue           消息列表容器
  │   ├─ AiMessage.vue         纯文本，无气泡，左侧对齐
  │   ├─ UserMessage.vue       隐约橘色 pill，右侧对齐
  │   ├─ ScoreCard.vue         SSE 分段渲染：分数 → 正确点 → 遗漏 → 建议 → 追问
  │   ├─ FollowUpPrompt.vue    追问气泡 + [接受] [跳过] 按钮
  │   └─ ReviewButtons.vue     [想起] [不确定] [忘了] 三按钮
  └─ ChatInput.vue             输入区
```

**三种模式的交互差异**：
```
模块背诵 (CATEGORY):
  1. 开始前弹出模块选择 + 题数选择
  2. AI 出题（只出题干，不展示答案）
  3. 用户输入答案 → SSE 流式评分 → 展示结果
  4. 追问可选
  5. 点击"下一题"继续

随机背诵 (RANDOM):
  1. 开始前选择模块范围（可多选）+ 题数
  2. 同模块背诵流程

今日复习 (REVIEW):
  1. 无需选择，自动取到期 10 题
  2. AI 同时展示题目 + 答案（复习模式直接看答案）
  3. 用户自评：想起 / 不确定 / 忘了
  4. 无 LLM 评分，无追问
```

---

### 功能 3：SSE 流式评分展示

**怎么实现**：

```
submitAnswer 返回的是 SSE 流，不是普通 JSON。

前端接收:
  const eventSource = new EventSource(`/api/recite/${sid}/answer?...`)

  实际因 POST 限制，改为:
  fetch('/api/recite/${sid}/answer', {method:'POST', body:...})
    .then(res → {
      const reader = res.body.getReader()
      // 手动解析 text/event-stream
    })

  或更简单: Controller 返回 SseEmitter，前端用 fetch + ReadableStream 读取

  逐段接收事件:
  event: score     → data: {score:8}        → 分数数字跳动动画
  event: correct   → data: {points:[...]}   → 正确点逐条出现
  event: missed    → data: {points:[...]}   → 遗漏点标红
  event: suggestion→ data: {text:"..."}     → 建议淡入
  event: followUp  → data: {question:"..."} → 追问气泡弹出
  event: done      → data: {recordId:42}    → 显示"下一题"按钮
```

**ScoreCard 组件状态机**：
```
WAITING → SCORING(分数数字 0→8 跳动) → CORRECT(逐条) → 
MISSED(逐条) → SUGGESTION → FOLLOWUP → DONE
```

---

### 功能 4：报告展示

- finishRecite 后，消息列表底部展示基础报告（总分、平均分、题数）
- 轮询 `/report/{sessionId}` → 拿到后替换为完整报告（AI 评语 + 模块条 + 建议）
- 轮询 `/achievement/new` → Toast 新徽章

---

### 功能 5：徽章墙

**页面**：`/achievements`

**布局**：分类卡片网格

```
┌────────────────────────────────────────────┐
│  Toast 通知区 (position: fixed, top)        │
├────────────────────────────────────────────┤
│                                            │
│  ┌─ 背诵量 ──────────────────────────────┐ │
│  │ [百题斩 🥇] [初出茅庐] [题海勇士] ...  │ │
│  └────────────────────────────────────────┘ │
│  ┌─ 质量 ────────────────────────────────┐ │
│  │ [优秀学者] [满分收割机] ...             │ │
│  └────────────────────────────────────────┘ │
│  ...                                        │
│                                            │
│  已获得: 金色 icon + 获得时间               │
│  未获得: 灰色 + 进度条 (67/100 → 67%)       │
│  隐藏: 问号 icon，获得后展示                │
└────────────────────────────────────────────┘
```

**组件**：
```
AchievementWall.vue
  ├─ ToastContainer.vue        新徽章通知（3 秒自动消失，多枚逐个弹出）
  └─ BadgeGrid.vue
       └─ BadgeCard.vue × 46   点击弹出 BadgeDetail 模态框
```

**Toast 交互**：
```
finishRecite → 轮询 GET /achievement/new
  → 返回 [{key:"total_100", name:"百题斩"}, ...]
  → 逐个展示 Toast（每枚 3 秒）
  → POST /achievement/new/ack
```

---

### 功能 6：管理后台

**模块管理页** `/admin/modules`：
- 模块列表表格（模块名、key、状态、题目数）
- 新建模块弹窗
- 上下线切换
- 点击进入题目列表

**运维监控页** `/admin/monitor`：
- 今日统计卡（请求数、平均耗时、错误数）
- 链路列表（traceId、方法、状态、耗时）
- 点击展开单条链路节点详情（树形耗时条）

---

### 功能 7：Pinia Store

```
authStore:
  state:  token, user, role
  actions: login(), register(), adminLogin(), logout()

reciteStore:
  state:  sessionId, mode, messages[], currentQuestion,
          isStreaming, currentIndex, totalQuestions
  actions: startRecite(), submitAnswer(), submitFollowUp(),
           finishRecite(), fetchHistory()

achievementStore:
  state:  allBadges[], earnedBadgeKeys[], newBadges[]
  actions: fetchBadges(), pollNewBadges(), ackNewBadges()
```

---

## 三、路由设计

```javascript
const routes = [
  { path: '/login',          component: Login },
  { path: '/recite',         component: ChatRecite,      meta: {auth: true} },
  { path: '/achievements',   component: AchievementWall, meta: {auth: true} },
  { path: '/admin/modules',  component: AdminModules,    meta: {auth: true, role:'ADMIN'} },
  { path: '/admin/monitor',  component: AdminMonitor,    meta: {auth: true, role:'ADMIN'} },
  { path: '/',               redirect: '/recite' },
]

router.beforeEach((to, from, next) => {
  if (!to.meta.auth) return next()
  if (localStorage.getItem('token')) return next()
  next('/login')
})
```

---

## 四、API 层（axios）

```javascript
// api/index.js
const api = axios.create({ baseURL: '/api', timeout: 30000 })

// 请求拦截：自动带 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = token
  return config
})

// 响应拦截：统一错误处理
api.interceptors.response.use(
  res => {
    if (res.data.code && res.data.code !== '0') {
      if (res.data.code === '401') { /* 跳转登录 */ }
      return Promise.reject(res.data.message)
    }
    return res.data  // 直接返回 Response.data
  },
  err => Promise.reject(err)
)

export default {
  // auth
  login, register, adminLogin,
  // recite
  startRecite, submitAnswer(sid, data) → SSE, submitFollowUp, finishRecite,
  getSession, getHistory,
  // knowledge
  listModules, createModule, updateModuleStatus, deleteModule,
  listQuestions, updateQuestion, triggerImport,
  // report
  getDashboard, getReport(sid), getJournal,
  // achievement
  getAllBadges, getBadgeDetail, getNewBadges, ackNewBadges,
  // monitor
  listTraces, getTraceDetail, getMonitorStats, cleanTraces,
}
```

**SSE 请求特殊处理**：
```javascript
// submitAnswer 不走 axios，用 fetch + ReadableStream
async function submitAnswer(sid, questionId, answer) {
  const res = await fetch(`/api/recite/${sid}/answer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': token },
    body: JSON.stringify({ questionId, answer })
  })
  // 返回 ReadableStream，由调用方逐 chunk 读取
  return res.body.getReader()
}
```

---

## 五、视觉规范

| 元素 | 样式 |
|---|---|
| 背景 | 暖灰 `#f5f0eb` |
| 主色调 | 橘珊瑚 `#f97316` |
| AI 消息 | 纯文本，无气泡，无头像，左对齐 |
| 用户消息 | 隐约橘色 pill `bg-orange-100 text-orange-900`，右对齐 |
| 发送按钮 | 橘珊瑚底色，白色图标 |
| 输入框 | `bg-white border border-gray-200 rounded-xl`，聚焦时 `border-orange-400` |
| 分数动画 | `text-4xl font-bold text-orange-500` 数字跳动 |
| 徽章卡片 | 已获得: 金色边框 + 彩色 icon / 未获得: 灰色 + 进度条 |
| Toast | `fixed top-4 right-4`，金色背景，3 秒自动消失 |

---

## 六、文件清单

```
frontend/
├── index.html
├── vite.config.js
├── tailwind.config.js
├── package.json
│
└── src/
    ├── main.js                        入口
    ├── App.vue                        根组件
    │
    ├── api/
    │   └── index.js                   axios 封装 + 全部 API
    │
    ├── router/
    │   └── index.js                   路由定义 + 守卫
    │
    ├── stores/
    │   ├── authStore.js               token, user, role
    │   ├── reciteStore.js             会话, 消息, SSE 状态
    │   └── achievementStore.js        徽章, Toast 队列
    │
    ├── views/
    │   ├── Login.vue                  登录/注册
    │   ├── ChatRecite.vue             背诵对话（核心）
    │   ├── AchievementWall.vue        徽章墙
    │   ├── AdminModules.vue           模块管理
    │   └── AdminMonitor.vue           运维监控
    │
    ├── components/
    │   ├── chat/
    │   │   ├── MessageList.vue        消息列表
    │   │   ├── AiMessage.vue          AI 纯文本消息
    │   │   ├── UserMessage.vue        用户橘色 pill
    │   │   ├── ScoreCard.vue          SSE 评分卡片
    │   │   ├── FollowUpPrompt.vue     追问交互
    │   │   ├── ReviewButtons.vue      自评三按钮
    │   │   ├── ChatInput.vue          输入框 (Enter/Shift+Enter/IME)
    │   │   ├── ReciteTopBar.vue       顶部模式切换
    │   │   └── ModeSelector.vue       模式/模块/题数选择
    │   ├── achievement/
    │   │   ├── BadgeGrid.vue          徽章网格
    │   │   ├── BadgeCard.vue          单枚徽章
    │   │   └── BadgeDetail.vue        徽章详情模态框
    │   ├── admin/
    │   │   ├── ModuleTable.vue        模块表格
    │   │   ├── ModuleForm.vue         新建/编辑模块
    │   │   └── TraceDetail.vue        链路详情展开
    │   └── common/
    │       ├── ToastContainer.vue     Toast 通知容器
    │       └── NavBar.vue             导航栏
    │
    └── assets/
        └── style.css                  Tailwind + 自定义
```

**总计 ~30 个文件**（5 页面 + 15 组件 + 3 store + router + api + 配置）

---

## 七、编码顺序

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `vite.config.js` `tailwind.config.js` `package.json` | 项目骨架 |
| 2 | `main.js` `App.vue` `style.css` | 入口 |
| 3 | `api/index.js` | 后端通信基础 |
| 4 | `router/index.js` | 路由 |
| 5 | `stores/authStore.js` | 认证状态 |
| 6 | `Login.vue` | 先打通登录 |
| 7 | `stores/reciteStore.js` | 背诵核心状态 |
| 8 | `ChatInput.vue` `MessageList.vue` `AiMessage.vue` `UserMessage.vue` | 基础聊天组件 |
| 9 | `ScoreCard.vue` `FollowUpPrompt.vue` `ReviewButtons.vue` | 评分交互 |
| 10 | `ReciteTopBar.vue` `ModeSelector.vue` → `ChatRecite.vue` | 组装背诵页 |
| 11 | `stores/achievementStore.js` + 徽章组件 | 徽章功能 |
| 12 | `AchievementWall.vue` | 徽章页 |
| 13 | `AdminModules.vue` `AdminMonitor.vue` | 管理后台 |
| 14 | `ToastContainer.vue` `NavBar.vue` | 公共组件 |

每步 `npm run dev` 验证。
