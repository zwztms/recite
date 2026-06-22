# 翻卡学习 + 个人主页 设计文档

> 日期：2026-06-22 | 状态：设计完成，待开始实施

---

## 一、概述

在现有"对话背诵"基础上新增两种学习方式：
1. **个人主页** — 登录后首屏，学习概览 + 双入口
2. **翻卡学习** — 题目列表自由浏览，侧边索引快速跳转，点击展开答案，随手标记掌握度

---

## 二、页面与路由

| 路径 | 页面 | 权限 | 说明 |
|------|------|:--:|------|
| `/home` | HomePage.vue | 需登录 | **新增**，登录后默认首页 |
| `/learn` | CardLearn.vue | 需登录 | **新增**，翻卡学习 |
| `/recite` | ChatRecite.vue | 需登录 | 现有，不变 |
| `/achievements` | AchievementWall.vue | 需登录 | 现有，不变 |
| `/admin/modules` | AdminModules.vue | 管理员 | 现有，不变 |
| `/admin/monitor` | AdminMonitor.vue | 管理员 | 现有，不变 |
| `/login` | Login.vue | 公开 | 现有，不变 |
| `/` | 重定向到 `/home` | — | **修改**，原重定向到 `/recite` |

---

## 三、个人主页 `/home`

### 3.1 页面结构

```
┌─ 导航栏 ──────────────────────────────────┐
│  首页 | 学习 | 背诵 | 成就 | 管理          │
├─ 问候 + 统计条 ────────────────────────────┤
│  头像 昵称  连续天数 | 累计背诵 | 已掌握 | 总进度  │
├─ 双 CTA ──────────────────────────────────┤
│  [翻卡学习] (主按钮)  [对话背诵] (次按钮)     │
├─ 三列卡片 ────────────────────────────────┤
│  模块掌握(左)  7天趋势(中)  徽章+标签+建议(右)  │
│  55/69        柱状图       3枚徽章            │
│  19/32                     薄弱标签           │
│  ...                       AI 建议            │
├─ 最近背诵 ────────────────────────────────┤
│  时间 | 模块 · 题数 · 均分 | 查看报告 →      │
│  (点击弹出报告弹窗)                          │
└──────────────────────────────────────────┘
```

### 3.2 后端 API

**新增：`GET /api/home/dashboard`**

聚合以下数据，一次返回：

| 字段 | 来源 | 现有/新增 |
|------|------|:--:|
| user.nickname | users 表 → UserPort | 现有 |
| stats.streakDays | user_streak 表 → StreakPort | 现有 |
| stats.totalRecites | recite_records 计数 → ReciteRecordPort.countByUserId | 现有 |
| stats.masteredCount | user_progress WHERE masteryScore >= 80 → ProgressPort.countMastered | 现有 |
| stats.totalProgress | masteredCount / 全题库总数 | 现有 |
| moduleMastery[] | 每个模块：掌握数(progress) / 总题数(question_vectors) | 现有 Port 组合 |
| trend[] | 近7天每日背诵题数 | **新增查询**（recite_records GROUP BY date） |
| badges[] | 最近3枚已获得徽章 → AchievementPort | 现有 |
| weakTags | 最近 journal 中高频弱项 → ReportPort | 现有 |
| advice | 最新一条 AI 建议 → ReportPort | 现有 |
| recentRecites[] | 最近5条背诵记录 → ReciteRecordPort | 现有 |

- **不新增数据库表**
- **不新增 SPI 接口**（复用现有 Port 查询）
- 新增 1 个 Controller 方法（可放现有 ReportController 或新建 HomeController）
- 新增 1 个 DTO：`HomeDashboardDTO`

### 3.3 前端

- **新建**：`frontend/src/views/HomePage.vue`
- **新建**：`frontend/src/components/home/ReportModal.vue` — 报告弹窗，复用 `GET /report/{sessionId}`

---

## 四、翻卡学习 `/learn`

### 4.1 页面结构

```
┌─ 控制栏 ───────────────────────────────────┐
│ [模块 ▼] [顺序 | 乱序] [全部|未掌握|已掌握]  │
├─ 主体 (左右分栏) ──────────────────────────┤
│ 左侧索引 (180px)    │ 右侧内容区            │
│ ┌─────────────────┐│ ┌───────────────────┐ │
│ │ 1. 事务ACID   ● ││ │③ 事务ACID  ⭐⭐   │ │
│ │ 2. 索引类型   ● ││ │  答案展开区        │ │
│ │ 3. SQL注入    ○ ││ │  [已掌握][未掌握]  │ │
│ │ 4. MVCC机制   ○ ││ ├───────────────────┤ │
│ │ 5. 分库分表   ○ ││ │④ SQL注入原理  ⭐⭐│ │
│ │ ...            ││ │  点击展开          │ │
│ └─────────────────┘│ │ ...               │ │
│                     │ └───────────────────┘ │
└─────────────────────────────────────────────┘
```

### 4.2 交互

| 操作 | 行为 |
|------|------|
| 点击索引项 | 右侧滚动到对应题目 |
| 滚动内容区 | 索引联动高亮当前可见题 |
| 点击题目行 | 手风琴展开答案（只展开一个） |
| 点击已展开题 | 收起 |
| 按 ESC | 收起当前展开 |
| 标记"已掌握" | 调用后端，更新 user_progress（masteryScore=80+），索引绿点变亮 |
| 标记"未掌握" | 调用后端，更新 user_progress（masteryScore=30），索引绿点变灰 |

### 4.3 后端 API

**新增：`GET /api/learn/questions?moduleKey=&order=seq|random&filter=all|unmastered|mastered`**

返回题目列表（含掌握状态）：

| 字段 | 来源 |
|------|------|
| questions[] | QuestionPort.searchByModule 或 searchRandom |
| 每题 mastered | 查 user_progress 表 masteryScore >= 80 |

**新增：`POST /api/learn/mark`** `{ questionId, mastered: true/false }`

标记掌握度，调用 `SpacedRepetitionService.calculateAfterScore()` 更新间隔重复数据。

- **不新增数据库表**
- **不新增 SPI 接口**
- 新增 2 个 API 端点
- 新增 1 个 Controller（LearnController）
- 复用：QuestionPort、ProgressPort、SpacedRepetitionService

### 4.4 前端

- **新建**：`frontend/src/views/CardLearn.vue`
- **新建**：`frontend/src/components/learn/SideIndex.vue` — 侧边索引
- **新建**：`frontend/src/components/learn/QuestionCard.vue` — 题目卡片（展开/收起）

---

## 五、导航重构

### 5.1 改动

| 文件 | 改动 |
|------|------|
| `App.vue` | 导航链接：首页→/home，学习→/learn，背诵→/recite，成就→/achievements |
| `router/index.js` | 新增 /home、/learn 路由；"/" 重定向改为 /home；导航守卫不变 |
| `authStore.js` | 登录成功后跳转改为 /home |

### 5.2 影响

- 现有背诵、成就、管理后台路由不变
- 登录后默认首页从 /recite 改为 /home
- 导航守卫鉴权逻辑不变

---

## 六、Phase 划分

| Phase | 内容 | 新文件 | 新表 | 影响旧代码 |
|:--:|------|:--:|:--:|:--:|
| 12 | 个人主页后端 API | ~3 | 无 | 无（新 Controller + DTO） |
| 13 | 个人主页前端 | ~2 | 无 | router 加 /home 路由 |
| 14 | 翻卡学习后端 API | ~3 | 无 | 无（新 Controller + DTO） |
| 15 | 翻卡学习前端 | ~3 | 无 | 无 |
| 16 | 导航重构 | 0 | 无 | App.vue + router + authStore（3 文件小改） |

---

## 七、不涉及的内容

- 不新建数据库表
- 不新增 SPI 接口（全部复用现有 Port）
- 不修改现有背诵流程（/recite 完全不变）
- 不修改现有 Docker Compose / 中间件配置
