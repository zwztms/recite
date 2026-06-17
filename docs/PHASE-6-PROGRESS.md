# Phase 6 — progress 进度 实施计划

> 日期：2026-06-16 | 状态：计划已审批，待编码

---

## 一、子域定位

progress 是核心域。负责三件事：**掌握度追踪**、**间隔重复算法**、**连续天数**。它不主动发起动作，被 recite 子域同步调用，被 report 子域查询。不对外暴露 REST API。

---

## 二、要实现哪些功能，怎么实现

### 功能 1：AI 评分后更新掌握度 + 间隔

**做什么**：每次 AI 评分完一道题（CATEGORY / RANDOM 模式），更新该题的 mastery_score，根据得分计算下次复习时间。

**完整调用链路**：

```
前端提交答案
  │
  ▼
ReciteController.submitAnswer(sid, {questionId, answer})
  │
  ▼
ReciteOrchestrationService.submitAnswer(userId, sid, questionId, answer)
  │
  ├─ [Phase 5 逻辑] 校验、抢槽、LLM评分、存recite_records
  │
  └─ 评分完成后同步调用:
       │
       ▼
     SpacedRepetitionService.calculateAfterScore(current, aiScore)
       │
       ├─① 查当前进度
       │   ProgressPort.findByUserAndQuestion(userId, questionId)
       │   └→ SELECT * FROM user_progress WHERE user_id=? AND question_id=?
       │      返回 null → 首次背诵 / 返回实体 → 基于旧值计算
       │
       ├─② 计算 mastery_score（加权平滑）
       │   首次: masteryScore = aiScore × 10          (例: 8分→80)
       │   非首次: masteryScore = 旧分×0.7 + (aiScore×10)×0.3
       │           例: 旧50, 本次8分 → 50×0.7+80×0.3 = 59
       │           clamp(0, 100)
       │
       ├─③ 根据分数分档调整间隔
       │
       │   ┌─────────┬──────────────────────────────────────┐
       │   │ aiScore │ 算法                                  │
       │   ├─────────┼──────────────────────────────────────┤
       │   │  ≥ 8    │ interval = interval × easeFactor      │
       │   │         │ easeFactor = min(easeFactor+0.1, 5.0) │
       │   │         │ → 掌握好，间隔拉长                     │
       │   ├─────────┼──────────────────────────────────────┤
       │   │  5-7    │ interval = max(interval, 1)            │
       │   │         │ easeFactor 不变                        │
       │   │         │ → 正常，间隔维持                       │
       │   ├─────────┼──────────────────────────────────────┤
       │   │  ≤ 4    │ interval = 1 (重置)                    │
       │   │         │ easeFactor = max(easeFactor-0.2, 1.3) │
       │   │         │ → 回答差，明天必须复习                  │
       │   └─────────┴──────────────────────────────────────┘
       │
       │   首次背诵: interval=1, easeFactor=2.5
       │
       ├─④ 计算 nextReviewAt = NOW() + interval 天
       │
       ├─⑤ 更新 reciteCount 和 averageScore
       │   reciteCount = old + 1
       │   averageScore = (旧平均×旧次数 + aiScore) / 新次数
       │
       └─⑥ 持久化
           ProgressPort.save(entity) 或 ProgressPort.update(entity)
           └→ INSERT/UPDATE user_progress
               SET mastery_score=?, review_interval=?, ease_factor=?,
                   next_review_at=?, recite_count=?, average_score=?
```

---

### 功能 2：REVIEW 模式自评更新

**做什么**：今日复习模式不调 LLM，用户自己判断"想起/不确定/忘了"，根据自评结果调整间隔。

**完整调用链路**：

```
前端今日复习页 → 展示题目+答案 → 用户点击按钮

"想起" / "不确定" / "忘了"
  │
  ▼
ReciteController.submitAnswer(sid, {questionId, selfAssessment})
  │  (REVIEW 模式检测 → 不走 LLM)
  │
  ▼
ReciteOrchestrationService.submitAnswer(...)
  │  mode==REVIEW → 走自评分支
  │
  └─ 同步调用:
       │
       ▼
     SpacedRepetitionService.calculateAfterSelfAssessment(current, assessment)
       │
       ├─① 查当前进度
       │   ProgressPort.findByUserAndQuestion(userId, questionId)
       │
       ├─② 按自评结果调整
       │
       │   ┌──────────────┬────────────────────────────────────────┐
       │   │ "想起"        │ interval = interval × easeFactor        │
       │   │ (remembered)  │ easeFactor = min(easeFactor+0.1, 5.0)  │
       │   │               │ masteryScore = min(masteryScore+3, 100) │
       │   ├──────────────┼────────────────────────────────────────┤
       │   │ "不确定"      │ interval = max(ceil(interval×0.5), 1)   │
       │   │ (unsure)      │ easeFactor = max(easeFactor-0.15, 1.3)  │
       │   │               │ masteryScore 不变                       │
       │   ├──────────────┼────────────────────────────────────────┤
       │   │ "忘了"        │ interval = 1 (重置为1天)                │
       │   │ (forgot)      │ easeFactor = max(easeFactor-0.3, 1.3)   │
       │   │               │ masteryScore = max(masteryScore-10, 0)  │
       │   └──────────────┴────────────────────────────────────────┘
       │
       ├─③ nextReviewAt = NOW() + interval 天
       │
       └─④ 持久化
           ProgressPort.update(entity)
           └→ UPDATE user_progress SET ...
```

> REVIEW 模式不调 LLM，recite_records.score 字段为 null，仅写 self_assessment 标记。

---

### 功能 3：今日复习出题

**做什么**：REVIEW 模式开始时，查出今天该复习的到期题目。

**完整调用链路**：

```
POST /recite/start  {mode: "REVIEW"}

ReciteController.startRecite(dto)
  │
  ▼
ReciteOrchestrationService.startRecite(userId, REVIEW, ..., count)
  │
  ├─ mode == REVIEW:
  │
  └─ 同步查询:
       │
       ▼
     ProgressPort.findDueQuestions(userId, limit=10)
       │
       └→ SELECT * FROM user_progress
           WHERE user_id=? AND next_review_at <= NOW()
           ORDER BY next_review_at ASC
           LIMIT 10

       返回 List<UserProgressEntity>（到期题目）
       │
       ▼
     用 questionIds → QuestionPort 逐条查完整题目
       │
       ▼
     创建 ReciteSession (mode=REVIEW, questionIds=[...])
     存入 Redis recite:session:{sid}
       │
       ▼
     返回首题 + 答案给前端（REVIEW 直接展示答案）
```

---

### 功能 4：连续天数签到

**做什么**：每次结束背诵时更新连续天数。

**完整调用链路**：

```
POST /recite/{sid}/finish

ReciteController.finishRecite(sid)
  │
  ▼
ReciteOrchestrationService.finishRecite(userId, sid)
  │
  ├─ [Phase 5 逻辑] 查记录、统计、LLM 报告
  │
  └─ 同步签到:
       │
       ▼
     StreakService.checkIn(userId)
       │
       ├─① 查当前记录
       │   StreakPort.findByUserId(userId)
       │   └→ SELECT * FROM user_streak WHERE user_id=?
       │
       ├─② 判断连续性
       │
       │   lastActiveDate == 今天  → 不变（同天多次不重复计）
       │   lastActiveDate == 昨天  → currentStreak += 1
       │                             longestStreak = max(最长, 当前)
       │   lastActiveDate < 昨天   → currentStreak = 1 (断签)
       │   首次(null)             → currentStreak=1, longestStreak=1
       │
       ├─③ lastActiveDate = TODAY
       │
       └─④ 持久化
           StreakPort.save(entity) 或 StreakPort.update(entity)
           └→ INSERT/UPDATE user_streak
               SET current_streak=?, longest_streak=?, last_active_date=?
```

---

## 三、涉及存储

| 存储 | 表 | 操作 |
|---|---|---|
| PostgreSQL | `user_progress` | 增、改、查 |
| PostgreSQL | `user_streak` | 增、改、查 |

全部同步写 PG，无 Redis 和 MQ。

---

## 四、类清单与关系

```
┌──────────────────────────────────────────────────────────────────┐
│               recite-domain/progress (领域层)                      │
│                                                                   │
│  model/entity/                                                    │
│  ├─ UserProgressEntity    掌握度实体                               │
│  └─ UserStreakEntity      连续天数实体                             │
│                                                                   │
│  port/out/ (SPI — infra 实现)                                     │
│  ├─ ProgressPort         掌握度 CRUD                               │
│  └─ StreakPort           连续天数 CRUD                             │
│                                                                   │
│  service/                                                         │
│  ├─ SpacedRepetitionService  间隔重复算法（纯函数）                 │
│  └─ StreakService            连续天数逻辑                          │
│                                                                   │
│  exception/                                                       │
│  └─ ProgressException                                            │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│             recite-infrastructure/adapter/persistence/            │
│                                                                   │
│  ProgressAdapter    implements ProgressPort                       │
│    └→ UserProgressDO + UserProgressMapper                        │
│                                                                   │
│  StreakAdapter      implements StreakPort                         │
│    └→ UserStreakDO + UserStreakMapper                            │
└──────────────────────────────────────────────────────────────────┘
```

无 recite-api 和 recite-trigger —— progress 不对外暴露 REST API。

---

## 五、每个类/接口的详细职责

### 5.1 实体

#### UserProgressEntity
```java
// 映射 user_progress 表
Long id;
Long userId;
String questionId;
String moduleKey;
int masteryScore;           // 0-100，替代旧 MasteryLevel 枚举
int reciteCount;
double averageScore;
LocalDateTime lastRecitedAt;
LocalDateTime nextReviewAt; // ← 新增
int reviewInterval;         // ← 新增，范围 1-365
double easeFactor;          // ← 新增，范围 1.3-5.0，默认 2.5
```

#### UserStreakEntity
```java
// 映射 user_streak 表
Long userId;              // 主键
int currentStreak;
LocalDate lastActiveDate;
int longestStreak;
```

### 5.2 Port 接口

```java
public interface ProgressPort {
    Optional<UserProgressEntity> findByUserAndQuestion(Long userId, String questionId);
    List<UserProgressEntity> findByUserId(Long userId);
    List<UserProgressEntity> findDueQuestions(Long userId, int limit);
    // SQL: WHERE user_id=? AND next_review_at <= NOW() ORDER BY next_review_at ASC LIMIT ?
    void save(UserProgressEntity progress);

    // ---- M-fM-^SM-^MM-eM-^PM-^HM-gM-;M-^_M-hM-.M-!M-fM-^UM-0M-oM-<M-^HM-gM-^TM-( Phase 7/8 M-hM-7M-(M-eM-^_M-^_M-hM-^CM-=M-hM-/M-"M-oM-<M-^I ----
    int countMastered(Long userId);
    // SQL: SELECT COUNT(*) FROM user_progress WHERE user_id=? AND mastery_score >= 80
    void update(UserProgressEntity progress);
}

public interface StreakPort {
    Optional<UserStreakEntity> findByUserId(Long userId);
    void save(UserStreakEntity streak);
    void update(UserStreakEntity streak);
}
```

### 5.3 领域服务

```java
public class SpacedRepetitionService {
    // AI 评分后计算。纯函数，不调外部，不写 DB。返回更新后的实体由调用方持久化。
    UserProgressEntity calculateAfterScore(UserProgressEntity current, int aiScore);

    // 自评后计算（REVIEW 模式）
    UserProgressEntity calculateAfterSelfAssessment(UserProgressEntity current, String assessment);
}

public class StreakService {
    // 每日签到，计算连续天数
    UserStreakEntity checkIn(Long userId);
}
```

### 5.4 基础设施

| 适配器 | 实现 | 技术 |
|---|---|---|
| `ProgressAdapter` | `ProgressPort` | MyBatis Plus，操作 `user_progress` 表 |
| `StreakAdapter` | `StreakPort` | MyBatis Plus，操作 `user_streak` 表 |

---

## 六、与 recite 的调用关系

```
ReciteOrchestrationService
  │
  ├─ submitAnswer() 中:
  │   ├→ SpacedRepetitionService.calculateAfterScore()
  │   │    └→ ProgressPort 查 + 算 + 存
  │   └→ (REVIEW模式) calculateAfterSelfAssessment()
  │        └→ ProgressPort 查 + 算 + 存
  │
  ├─ startRecite(REVIEW) 中:
  │   └→ ProgressPort.findDueQuestions()
  │        └→ 用 questionIds 调 QuestionPort 取完整题目
  │
  └─ finishRecite() 中:
      └→ StreakService.checkIn()
           └→ StreakPort 查 + 算 + 存
```

---

## 七、文件清单

```
recite-v2/
│
├── recite-domain/src/main/java/cn/bugstack/recite/domain/progress/
│   ├── model/entity/
│   │   ├── UserProgressEntity.java
│   │   └── UserStreakEntity.java
│   ├── port/out/
│   │   ├── ProgressPort.java
│   │   └── StreakPort.java
│   ├── service/
│   │   ├── SpacedRepetitionService.java
│   │   └── StreakService.java
│   └── exception/
│       └── ProgressException.java
│
└── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/adapter/
    └── persistence/
        ├── UserProgressDO.java
        ├── UserProgressMapper.java
        ├── UserStreakDO.java
        ├── UserStreakMapper.java
        ├── ProgressAdapter.java
        └── StreakAdapter.java
```

**总计 ~14 个文件**（8 domain + 6 infra）。

---

## 八、编码顺序

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `ProgressException.java` | 其他类依赖 |
| 2 | `UserProgressEntity.java` `UserStreakEntity.java` | 纯数据结构 |
| 3 | `ProgressPort.java` `StreakPort.java` | 领域契约 |
| 4 | `SpacedRepetitionService.java` | 核心算法，纯函数可独立验证 |
| 5 | `StreakService.java` | 签到逻辑 |
| 6 | DO + Mapper（4个） | 持久层 |
| 7 | `ProgressAdapter.java` `StreakAdapter.java` | 实现 Port |

每步完成后 `mvn compile` 验证。
