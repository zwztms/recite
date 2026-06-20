# 徽章系统简化计划 — 19 枚模块徽章 + 方案A金属渐变

> 日期：2026-06-20 | 基于原型：`docs/design/badge-module-19.html`

---

## 一、变更概要

| 维度 | 改造前 | 改造后 |
|------|------|------|
| 徽章数量 | 46 枚（38 公开 + 8 隐藏） | **19 枚**（每模块一枚） |
| 徽章分类 | 背诵量/质量/坚持/模块/组合/隐藏 | **单一：模块通关** |
| 徽章内容 | 名称+描述+emoji icon | **模块缩写**（JVM/JUC/SQL 等） |
| 视觉风格 | 金/灰两态卡片 | **四档金属渐变**（金/暗金/铜/紫金） |
| 获得条件 | 多样（累计题数/均分/连续天数/组合） | **统一：模块背诵 ≥ 20 题** |

---

## 二、后端影响

### 2.1 影响范围

| 文件 | 影响程度 | 变更描述 |
|------|:--:|------|
| `BadgeRegistry.java` | 🔴 大改 | 46 枚 → 19 枚，全部重新定义 |
| `AchievementService.java` | 🟡 中改 | 移除 calculateProgress 的分支逻辑（背诵量/质量/坚持/组合），只保留模块类 |
| `AchievementController.java` | 🟢 微调 | 移除 moduleCounts 查询分支（原本只为模块徽章查的，现在所有徽章都是模块类） |
| `UserStatsVO.java` | 🟢 无影响 | 字段保留（向后兼容），实际只用 `earnedModuleBadges` |
| `BadgeDefinition.java` | 🟢 无影响 | 结构不变 |
| `AchievementPort.java` | 🟢 无影响 | SPI 不变 |
| `NewBadgePort.java` | 🟢 无影响 | Redis 新徽章通知不变 |
| `achievement_log` 表 | 🟢 无影响 | 只存 badge_key，无需迁移 |
| `ReciteOrchestrationService.java` | 🟢 无影响 | 发放时机不变（submitAnswer/finishRecite 后 MQ 触发） |

### 2.2 不受影响的模块

- **数据库**：`achievement_log` 表不变，旧徽章记录保留（badge_key 不匹配的自动失效）
- **API 契约**：`GET /achievement/` 返回格式不变，只是数量减少
- **背诵流程**：`submitAnswer`/`finishRecite` 后的成就检查逻辑不变
- **其他子域**：recite / progress / report / knowledge 完全不动

### 2.3 数据迁移说明

- 已获得的旧徽章（如 `total_10`、`avg_7`、`streak_3`）的 `achievement_log` 记录保留但前端不再展示
- 19 枚新徽章的 badge_key 与旧模块徽章一致（如 `jvm`、`juc`），已获得的模块徽章自动继承

---

## 三、前端影响

| 文件 | 影响程度 | 变更描述 |
|------|:--:|------|
| `BadgeCard.vue` | 🔴 重写 | 方案A金属渐变背景 + 缩写icon + 四档配色 |
| `BadgeGrid.vue` | 🟡 简化 | 去掉分组逻辑，单网格 19 枚 |
| `BadgeDetail.vue` | 🟡 调整 | 展示模块名+缩写+获得日期 |
| `achievementStore.js` | 🟡 简化 | 移除 groupedBadges / 分类顺序逻辑 |
| `AchievementWall.vue` | 🟢 微调 | 移除分类分组，纯网格 |

---

## 四、19 枚徽章定义

| 缩写 | 模块名 | module_key | 等级 | 色系 |
|:--:|------|------|:--:|------|
| JVM | JVM 虚拟机 | jvm | 🥇 金 | 核心 |
| JUC | JUC 并发 | juc | 🥇 金 | 核心 |
| SQL | MySQL | mysql | 🥇 金 | 核心 |
| RED | Redis | redis | 🥇 金 | 核心 |
| SPR | Spring | spring | 🥇 金 | 核心 |
| JDK | Java 基础 | java-basics | 🏅 暗金 | Java生态 |
| COL | 集合框架 | java-collections | 🏅 暗金 | Java生态 |
| OS | 操作系统 | os | 🏅 暗金 | Java生态 |
| ALG | 数据结构与算法 | ds-algo | 🏅 暗金 | Java生态 |
| NET | 计算机网络 | network | 🏅 暗金 | Java生态 |
| FT | AI 微调 | ai-finetune | 🏅 暗金 | Java生态 |
| OC | OpenClaw | ai-openclaw | 🏅 暗金 | Java生态 |
| RAG | AI-RAG | ai-rag | 🥉 铜 | AI子域 |
| PRM | AI Prompt | ai-prompt | 🥉 铜 | AI子域 |
| EVL | AI 评估 | ai-eval | 🥉 铜 | AI子域 |
| SEC | AI 安全 | ai-security | 🥉 铜 | AI子域 |
| AIS | AI Spring | ai-spring | 💎 紫金 | AI综合 |
| AGT | AI Agent | ai-agent | 💎 紫金 | AI综合 |
| DSG | AI 设计 | ai-design | 💎 紫金 | AI综合 |

---

## 五、编码顺序

| 步骤 | 文件 | 内容 |
|:--:|------|------|
| 1 | `BadgeRegistry.java` | 替换 46 枚 → 19 枚，condition 统一为模块背诵 ≥ 20 题 |
| 2 | `AchievementService.java` | 简化 calculateProgress / evaluateAll |
| 3 | `AchievementController.java` | 移除 moduleCounts 分支 |
| 4 | `BadgeCard.vue` | 重写为方案A金属渐变卡片 |
| 5 | `BadgeGrid.vue` | 简化为单网格 |
| 6 | `achievementStore.js` | 移除分组/分类逻辑 |
| 7 | `AchievementWall.vue` | 微调整体布局 |
| 8 | `BadgeDetail.vue` | 调整详情弹窗 |

---

Co-Authored-By: Claude <noreply@anthropic.com>
