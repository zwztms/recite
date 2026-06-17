# Phase 6 编码报告 — progress 间隔重复子域

> 日期：2026-06-17 | 提交：e0b334b | 范围：掌握度追踪 + 间隔重复算法 + 连续天数签到

---

## 一、文件清单

### recite-domain/progress（7 个）

| 文件 | 职责 |
|------|------|
| `ProgressException.java` | 子域异常 |
| `UserProgressEntity.java` | 掌握度实体：masteryScore(0-100), interval, easeFactor, nextReviewAt |
| `UserStreakEntity.java` | 连续天数实体：currentStreak, lastActiveDate, longestStreak |
| `ProgressPort.java` | SPI：查/存/更掌握度 + 到期复习查询 + 已掌握统计 |
| `StreakPort.java` | SPI：查/存/更连续天数 |
| `SpacedRepetitionService.java` | 简化 FSRS 算法（纯函数）：AI 评分后计算间隔/掌握度 |
| `StreakService.java` | 签到服务：昨天连续→+1，断签→重置为1，同天不重复计 |

### recite-infrastructure/persistence（6 个）

| 文件 | 职责 |
|------|------|
| `UserProgressDO.java` | 映射 user_progress 表 |
| `UserProgressMapper.java` | BaseMapper + findDueQuestions + countMastered |
| `ProgressAdapter.java` | 实现 ProgressPort，MyBatis Plus |
| `UserStreakDO.java` | 映射 user_streak 表 |
| `UserStreakMapper.java` | BaseMapper |
| `StreakAdapter.java` | 实现 StreakPort，MyBatis Plus |

### recite-domain/recite（1 个修改）

| 文件 | 改动 |
|------|------|
| `ReciteOrchestrationService.java` | 注入 ProgressPort + SpacedRepetitionService + StreakService；submitAnswer() 后调间隔重复；finishRecite() 后调签到 |

---

## 二、间隔重复算法（简化 FSRS）

### 计算时机
`ReciteOrchestrationService.submitAnswer()` → AI 评分后同步调用 `SpacedRepetitionService.calculateAfterScore()`

### 算法

```
首次背诵:
  masteryScore = aiScore × 10       (例: 8分→80)
  interval     = 1 天
  easeFactor   = 2.5

非首次:
  masteryScore = 旧×0.7 + (aiScore×10)×0.3    (加权平滑, clamp 0-100)
  averageScore = 加权平均

  根据 aiScore 调整间隔:
    ≥8 → interval = interval × easeFactor, easeFactor += 0.1
    5-7 → interval 不变, easeFactor 不变
    ≤4 → interval = 1, easeFactor -= 0.2

  interval 范围: 1-365 天
  easeFactor 范围: 1.3-5.0
  nextReviewAt = now + interval 天
```

### REVIEW 模式
暂未实现——Phase 6 只处理 CATEGORY/RANDOM 评分后更新，REVIEW 模式的自评更新留待后续。

---

## 三、连续天数签到

### 调用时机
`ReciteOrchestrationService.finishRecite()` → 标记完成 → `StreakService.checkIn(userId)`

### 逻辑

```
lastActiveDate == 今天  → 不变（同天多次不重复计）
lastActiveDate == 昨天  → currentStreak += 1
lastActiveDate < 昨天   → currentStreak = 1（断签）
首次(null)             → currentStreak=1, longestStreak=1

longestStreak = max(longestStreak, currentStreak)
```

---

## 四、依赖注入变更

```
ReciteOrchestrationService（新增注入）
  ├─ SpacedRepetitionService  (progress 子域算法)
  ├─ ProgressPort             → ProgressAdapter (PG)
  └─ StreakService            → StreakAdapter (PG)
```

---

## 五、涉及数据库表

| 表 | 新增字段 | 说明 |
|------|------|------|
| `user_progress` | mastery_score, review_interval, ease_factor, next_review_at | 替代旧 MasteryLevel 枚举 |
| `user_streak` | — | 新表，以 userId 为主键 |

旧 recite V1 的 user_progress 表需 ALTER 加字段（部署时处理）。

---

## 六、编译结果

```
recite-types       SUCCESS
recite-api         SUCCESS
recite-domain      SUCCESS
recite-infrastructure SUCCESS
recite-trigger     SUCCESS
recite-app         SUCCESS
BUILD SUCCESS
```
