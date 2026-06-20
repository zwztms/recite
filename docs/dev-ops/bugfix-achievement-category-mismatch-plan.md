# 缺陷修复计划 — 成就墙分类名称不匹配

> 日期：2026-06-20 | 状态：已修复 | 提交：`01ad91a`

---

## 一、问题描述

**现象**：前端成就墙（AchievementWall）只展示 15 枚徽章，但页面顶部统计显示"总徽章 38 枚"。

**用户反馈**：面板上显示总徽章是 38 个，实际展示只有 15 个，怀疑是隐藏徽章或系统错误。

---

## 二、根因分析

### 2.1 数据流追踪

```
后端 BadgeRegistry.ALL_BADGES (46枚)
  ↓ BadgeRegistry.getPublicBadges() 过滤 hidden=true
  ↓ AchievementController.listAll() 返回至前端
  ↓
前端 achievementStore.fetchBadges()
  ↓ badges.value ← API 返回的 38 枚公开徽章
  ↓ groupedBadges 计算属性按 category 分组
  ↓ order.filter(k => groups[k]) 只保留 order 中存在于 groups 的分类
  ↓
BadgeGrid 渲染 → 实际只展示 15 枚
```

### 2.2 断点定位

问题出在 `achievementStore.js` 第 21 行的 `order` 数组。该数组定义分组渲染顺序，同时通过 `.filter(k => groups[k])` 隐式过滤不匹配的分类。

**前端 order 数组（旧）**：
```js
const order = ['背诵量', '质量', '坚持', '模块通关', '趣味 & 隐藏']
```

**后端 BadgeRegistry 返回的 category 值**：

| category | 徽章数 | 来源（BadgeRegistry） |
|------|:--:|------|
| `背诵量` | 5 | `BadgeRegistry` L38-47 |
| `质量` | 5 | L50-59 |
| `坚持` | 5 | L62-71 |
| `模块` | 19 | L74-130 |
| `组合` | 4 | L133-148 |
| `隐藏` | 8 | L151-176（API 已过滤） |

### 2.3 匹配分析

| order 值 | groups 中实际 key | 匹配 | 展示数 |
|------|------|:--:|:--:|
| `'背诵量'` | `'背诵量'` | ✅ | 5 |
| `'质量'` | `'质量'` | ✅ | 5 |
| `'坚持'` | `'坚持'` | ✅ | 5 |
| `'模块通关'` | `'模块'` | ❌ | 0 |
| `'趣味 & 隐藏'` | `'组合'` | ❌ | 0 |

**结果**：5 + 5 + 5 = 15 枚渲染，与用户观察完全吻合。缺失的 23 枚 = 19（模块）+ 4（组合）。

### 2.4 根因结论

Phase 10 编写 achievementStore 时，`order` 数组使用了与后端 API 不一致的分类名称。`order.filter(k => groups[k])` 在没有匹配项时静默丢弃数据，无任何 console 警告，导致问题难以发现。

---

## 三、修复方案

### 文件

`frontend/src/stores/achievementStore.js` — 第 21 行

### 改动

```diff
- const order = ['背诵量', '质量', '坚持', '模块通关', '趣味 & 隐藏']
+ const order = ['背诵量', '质量', '坚持', '模块', '组合']
```

### 设计原则

1. **命名一致性**：前端分��名必须与后端 `BadgeDefinition.category` 字段完全一致
2. **单一数据源**：后端 BadgeRegistry 是 category 值的唯一定义方，前端应直接沿用
3. **隐藏徽章不出现**：`隐藏` 类（8 枚）已由 `BadgeRegistry.getPublicBadges()` 过滤，不出现在公开 API 中，因此前端无需处理

### 改动范围

| 维度 | 值 |
|------|:--:|
| 修改文件 | 1 |
| 修改行数 | 1 |
| 后端改动 | 0 |
| 数据库改动 | 0 |
| API 改动 | 0 |

### 验证方式

- `vite build` 编译通过
- 浏览器打开成就墙，确认 5 个分类组全部展示：背诵量(5) + 质量(5) + 坚持(5) + 模块(19) + 组合(4) = 38 枚

---

## 四、预防措施

1. **常量抽取**：考虑将 category 定义抽取到共享常量文件（types 模块），前后端引用同一份定义
2. **防御性编码**：`order.filter().map()` 模式应在末尾加 `...其他未分类` 兜底组，避免静默丢弃数据
3. **集成测试**：成就墙渲染测试应验证 5 个分类 + 38 枚全部存在

---

Co-Authored-By: Claude <noreply@anthropic.com>
