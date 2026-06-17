# Phase 8 — achievement 成就 实施计划

> 日期：2026-06-16 | 状态：计划已审批，待编码

---

## 一、子域定位

achievement 是支撑域。负责徽章定义、条件评估、徽章发放、徽章查询。采用**异步事件驱动模式**——recite 的 finishRecite 发 MQ 消息后立即返回，achievement 消费者异步评估全部 46 枚徽章，前端轮询获取新徽章并弹 Toast。

---

## 二、要实现哪些功能，怎么实现

### 功能 1：异步徽章评估

**做什么**：每次结束背诵 → MQ 消费者评估用户是否满足新的徽章条件 → 命中则发放。

**完整调用链路**：

```
POST /recite/{sid}/finish

ReciteOrchestrationService.finishRecite(userId, sid)
  │
  ├─ ... 统计、报告 MQ ...
  │
  └─ 同时发 achievement MQ 消息:
      RocketMQ.send("recite-achievement-topic", {userId, sessionId})
      │
      └─ fire-and-forget，不阻塞 finishRecite 返回


       ═══════════ MQ 异步边界 ═══════════


AchievementConsumer.onMessage(msg)
  │  recite-infrastructure/adapter/mq/
  │
  ├─① 查用户全量数据（跨子域只读）
  │   ReciteRecordPort.countByUserId(userId)
  │     → 累计背诵总次数
  │   ReciteRecordPort.avgScoreByUserId(userId)
  │     → 历史平均分
  │   ReciteRecordPort.countByModule(userId, moduleKey)
  │     → 每个模块背诵次数
  │   ReciteRecordPort.countPerfectScores(userId)
  │     → 满分次数 (score=10)
  │   StreakPort.findByUserId(userId)
  │     → 当前连续天数、最长连续天数
  │   ProgressPort.countMastered(userId)
  │     → mastered 题数 (masteryScore≥80)
  │   AchievementPort.findEarnedBadgeKeys(userId)
  │     → 已获得的徽章 key 列表
  │
  ├─② 逐枚评估全部 46 枚徽章
  │   BadgeRegistry.ALL_BADGES.forEach(badge → {
  │       if (alreadyEarned(badge.key)) skip;
  │       if (badge.condition.evaluate(userStats)) {
  │           earnedBadges.add(badge);
  │       }
  │   });
  │
  │   每枚徽章定义:
  │   {
  │     key: "total_100",
  │     name: "百题斩",
  │     description: "累计背诵 100 题",
  │     icon: "...",
  │     category: "背诵量",
  │     hidden: false,
  │     condition: stats → stats.totalRecites >= 100
  │   }
  │
  ├─③ 写入 achievement_log
  │   每枚新徽章:
  │   AchievementPort.save(userId, badgeKey, NOW())
  │   └→ INSERT INTO achievement_log (user_id, badge_key, earned_at)
  │
  └─④ 标记新徽章（供前端轮询）
       Redis: SADD recite:new_badges:{userId}  badgeKey1 badgeKey2 ...
       Redis: EXPIRE recite:new_badges:{userId}  300 (5 分钟)
       │
       └→ 前端 GET /achievement/new 读取 SMEMBERS，展示 Toast 后 DEL


       ═══════════ MQ 异步边界 ═══════════


前端轮询（finishRecite 之后开始，每 2 秒）:
  │
  ▼
GET /achievement/new

AchievementController.getNewBadges()
  │
  ├─ Redis SMEMBERS recite:new_badges:{userId}
  │
  ├─ 返回 [{key, name, description, icon}]
  │
  └─ 前端展示 Toast 后:
      POST /achievement/ack  →  DEL Redis key
      （确认已读，不再重复弹）
```

### 功能 2：徽章墙

**做什么**：用户查看全部徽章（已获得金色 + 未获得灰显带进度）。

```
GET /achievement

AchievementController.listAll()
  │
  ├─① 查已获得徽章
  │   AchievementPort.findEarnedBadgeKeys(userId)
  │   └→ SELECT badge_key FROM achievement_log WHERE user_id=?
  │
  ├─② 查用户进度统计
  │   同消费者第一步，用于计算未获得徽章的进度百分比
  │   例: "百题斩" 需要 100 题，当前 67 题 → 进度 67%
  │
  ├─③ 组装返回
  │   全部 46 枚，每枚包含:
  │   {key, name, description, icon, category, hidden,
  │    earned: true/false, earnedAt,
  │    progress: {current: 67, target: 100, percent: 67}}
  │
  └─ 前端按 category 分组展示卡片网格
```

### 功能 3：徽章详情

```
GET /achievement/{badgeKey}

AchievementController.getDetail(badgeKey)
  ├─ 从 BadgeRegistry 取徽章定义
  ├─ 查是否已获得 + 获得时间
  └─ 返回完整信息（含详细描述、获得条件、进度）
```

---

## 三、徽章清单（46 枚）

### 背诵量（5 枚，公开）
| key | 名称 | 条件 |
|------|------|------|
| `total_10` | 初出茅庐 | 累计背诵 10 题 |
| `total_50` | 小有所成 | 累计背诵 50 题 |
| `total_100` | 百题斩 | 累计背诵 100 题 |
| `total_300` | 题海勇士 | 累计背诵 300 题 |
| `total_500` | 题库终结者 | 累计背诵 500 题 |

### 质量（5 枚，公开）
| key | 名称 | 条件 |
|------|------|------|
| `avg_7` | 稳定发挥 | 历史平均分 ≥ 7 |
| `avg_8` | 优秀学者 | 历史平均分 ≥ 8 |
| `avg_9` | 接近完美 | 历史平均分 ≥ 9 |
| `perfect_1` | 首战满分 | 首次获得 10 分 |
| `perfect_10` | 满分收割机 | 累计 10 次 10 分 |

### 坚持（5 枚，公开）
| key | 名称 | 条件 |
|------|------|------|
| `streak_3` | 三天打鱼 | 连续 3 天 |
| `streak_7` | 周不懈怠 | 连续 7 天 |
| `streak_14` | 半月坚持 | 连续 14 天 |
| `streak_30` | 月度之星 | 连续 30 天 |
| `streak_60` | 持之以恒 | 连续 60 天 |

### 模块单枚（19 枚，公开）
每个模块背诵 ≥ 20 题获得对应徽章。19 个模块：`java-basics`, `juc`, `jvm`, `java-collections`, `spring`, `mysql`, `redis`, `os`, `ds-algo`, `network`, `ai-rag`, `ai-spring`, `ai-finetune`, `ai-prompt`, `ai-eval`, `ai-security`, `ai-design`, `ai-openclaw`, `ai-agent`

### 组合（4 枚，公开）
| key | 名称 | 条件 |
|------|------|------|
| `combo_all_java` | Java 全栈 | 集齐 java-basics + juc + jvm + java-collections |
| `combo_all_ai` | AI 专家 | 集齐全部 9 个 AI 模块 |
| `combo_all_cs` | 计算机基础 | 集齐 os + ds-algo + network |
| `combo_all_modules` | 全模块制霸 | 集齐全部 19 个模块 |

### 趣味隐藏（8 枚，不公开条件）
| key | 名称 | 条件 |
|------|------|------|
| `hidden_night_owl` | 夜猫子 | 在凌晨 0-5 点完成一次背诵 |
| `hidden_speed` | 快枪手 | 单题答题 ≤ 30 秒 |
| `hidden_three_stars` | 三星上将 | 单次会话 3 题全满分 |
| `hidden_comeback` | 卷土重来 | 断签 7 天后再次连续 3 天 |
| `hidden_first_blood` | 第一滴血 | 第一次完成任何背诵 |
| `hidden_marathon` | 马拉松 | 单次会话 ≥ 20 题 |
| `hidden_sniper` | 狙击手 | 追问链达到 3 层 |
| `hidden_collector` | 收藏家 | 获得 30 枚徽章 |

---

## 四、涉及存储

| 存储 | Key/表 | 用途 | 读写 |
|---|---|---|---|
| PostgreSQL | `achievement_log` | 徽章获得记录 | 写+读 |
| Redis | `recite:new_badges:{userId}` | 待领取徽章队列，TTL 5min | 写+读+删 |
| RocketMQ | `recite-achievement-topic` | 异步评估触发 | 写 |
| PostgreSQL | `recite_records` | 消费者评估用 | 只读（跨子域） |
| PostgreSQL | `user_progress` | 消费者评估用 | 只读（跨子域） |
| PostgreSQL | `user_streak` | 消费者评估用 | 只读（跨子域） |

---

## 五、类清单与关系

```
┌──────────────────────────────────────────────────────────────────┐
│                     recite-api                                    │
│                                                                   │
│  IAchievementService   ← 4 个 REST 端点                           │
│  DTO × 3               ← BadgeDTO, BadgeDetailDTO, NewBadgeDTO   │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│                   recite-trigger                                  │
│                                                                   │
│  AchievementController  ← 实现 IAchievementService                │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│            recite-domain/achievement (领域层)                      │
│                                                                   │
│  model/entity/                                                    │
│  └─ AchievementLog         徽章获得记录                            │
│                                                                   │
│  model/valueobj/                                                  │
│  ├─ BadgeDefinition        徽章定义（key, name, desc, 条件函数）   │
│  └─ UserStatsVO            用户统计快照（聚合数据）                │
│                                                                   │
│  model/event/                                                     │
│  └─ AchievementRequestMessage   MQ 消息体                         │
│                                                                   │
│  port/out/                                                        │
│  ├─ AchievementPort        徽章记录 CRUD                           │
│  ├─ NewBadgePort           新徽章 Redis 操作                       │
│  └─ 复用其他 Phase 的 Port（只读调用，不重新定义）:
│      ├─ ReciteRecordPort   ← Phase 5 recite
│      ├─ ProgressPort       ← Phase 6 progress
│      └─ StreakPort         ← Phase 6 progress
│                                                                   │
│  service/                                                         │
│  ├─ BadgeRegistry          全部 46 枚徽章定义（硬编码）            │
│  └─ AchievementService     评估编排 + 查询逻辑                     │
│                                                                   │
│  exception/                                                       │
│  └─ AchievementException                                          │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│             recite-infrastructure/adapter                         │
│                                                                   │
│  persistence/                                                     │
│  ├─ AchievementPersistenceAdapter  implements AchievementPort     │
│  │    └→ AchievementLogDO + AchievementLogMapper                 │
│  └─ 跨子域只读复用 Phase 5 ReciteRecordAdapter + Phase 6 ProgressAdapter/StreakAdapter
│                                                                   │
│  cache/                                                           │
│  └─ NewBadgeRedisAdapter           implements NewBadgePort        │
│       └→ Redis SMEMBERS / SADD / DEL                             │
│                                                                   │
│  mq/                                                              │
│  └─ AchievementConsumer            评估触发消费者                  │
│       └→ @RocketMQMessageListener(topic="recite-achievement-topic")│
└──────────────────────────────────────────────────────────────────┘
```

---

## 六、每个类/接口的详细职责

### 6.1 实体

#### AchievementLog
```java
// 映射 achievement_log 表
Long id;
Long userId;
String badgeKey;
LocalDateTime earnedAt;
```

### 6.2 值对象

```java
// 徽章定义（硬编码 46 条）
public class BadgeDefinition {
    String key;              // "total_100"
    String name;             // "百题斩"
    String description;      // "累计背诵 100 题"
    String icon;             // icon 标识
    String category;         // "背诵量" / "质量" / "坚持" / "模块" / "组合" / "隐藏"
    boolean hidden;          // 隐藏徽章不提前展示条件
    Function<UserStatsVO, Boolean> condition;  // 评估函数
}

// 用户统计快照（传给 condition 函数）
public class UserStatsVO {
    int totalRecites;                     // 累计背诵次数
    double averageScore;                  // 历史平均分
    int perfectScoreCount;               // 满分次数
    int currentStreak;                    // 当前连续天数
    int longestStreak;                    // 最长连续天数
    int masteredCount;                    // masteryScore≥80 的题数
    int totalSessions;                    // 完成会话次数
    Set<String> earnedModuleBadges;       // 已获得模块徽章的 moduleKey
    Set<String> earnedBadgeKeys;         // 已获得全部徽章的 key
    // 以下用于隐藏徽章
    int lastSessionHour;                  // 上次会话的小时(0-23)
    int lastSessionAnswerSeconds;        // 上次会话最快答题秒数
    int lastSessionPerfectCount;         // 上次会话满分题数
    int lastSessionQuestionCount;        // 上次会话题数
    int lastSessionMaxFollowUpDepth;     // 上次会话最大追问深度
    boolean wasStreakBroken;             // 是否曾经断签≥7天后恢复
}
```

### 6.3 Port 接口

```java
public interface AchievementPort {
    void save(Long userId, String badgeKey, LocalDateTime earnedAt);
    List<String> findEarnedBadgeKeys(Long userId);
    int countByUserId(Long userId);
}

public interface NewBadgePort {
    void addNewBadges(Long userId, List<String> badgeKeys);  // SADD + EXPIRE
    List<String> getNewBadges(Long userId);                  // SMEMBERS
    void ackNewBadges(Long userId);                          // DEL
}
```

### 6.4 领域服务

```java
public class BadgeRegistry {
    // 46 枚徽章的完整定义
    List<BadgeDefinition> ALL_BADGES = List.of(
        new BadgeDefinition("total_10", "初出茅庐", ..., false, s → s.totalRecites >= 10),
        new BadgeDefinition("total_50", "小有所成", ..., false, s → s.totalRecites >= 50),
        // ... 全部 46 枚
    );

    BadgeDefinition getByKey(String key);
    List<BadgeDefinition> getByCategory(String category);
    List<BadgeDefinition> getPublicBadges();     // 不含隐藏徽章
}

public class AchievementService {
    // 评估全部徽章（消费者调用）
    List<BadgeDefinition> evaluateAll(UserStatsVO stats);
    // 计算进度（查询调用）
    BadgeProgress calculateProgress(BadgeDefinition badge, UserStatsVO stats);
}
```

### 6.5 REST 接口

`IAchievementService`，路径 `/achievement`：

| # | 方法 | HTTP | 路径 | 说明 |
|:--:|------|------|------|------|
| 1 | `listAll` | GET | `/` | 全部徽章 + 进度 |
| 2 | `getDetail` | GET | `/{badgeKey}` | 单枚详情 |
| 3 | `getNewBadges` | GET | `/new` | 轮询新徽章 |
| 4 | `ackNewBadges` | POST | `/new/ack` | 确认已读 |

### 6.6 DTO

| DTO | 用途 |
|---|---|
| `BadgeDTO` | key, name, description, icon, category, hidden, earned, earnedAt, progress(current, target, percent) |
| `BadgeDetailDTO` | BadgeDTO 全部字段 + detailedDescription + earnCondition |
| `NewBadgeDTO` | key, name, description, icon | （轮询返回，仅含已获得的新徽章） |

### 6.7 MQ 消费者

```java
@Component
@RocketMQMessageListener(
    topic = "recite-achievement-topic",
    consumerGroup = "recite-achievement-consumer"
)
public class AchievementConsumer implements RocketMQListener<AchievementRequestMessage> {

    @Autowired private AchievementService achievementService;
    @Autowired private AchievementPort achievementPort;
    @Autowired private NewBadgePort newBadgePort;
    // + 跨子域只读 Port

    @Override
    public void onMessage(AchievementRequestMessage msg) {
        // 1. 查全量统计数据 → UserStatsVO
        // 2. achievementService.evaluateAll(stats) → 新徽章列表
        // 3. 逐枚 achievementPort.save()
        // 4. newBadgePort.addNewBadges(userId, newBadgeKeys)
    }
}
```

---

## 七、前端交互流程

```
finishRecite 返回
  │
  ├─ 开始轮询 GET /achievement/new (每 2 秒)
  │
  ├─ 第 1 次: [] (消费者还在评估)
  ├─ 第 2 次: [{key:"total_100", name:"百题斩", ...}]
  │
  ├─ 停止轮询
  ├─ 顶部弹出 Toast (金色 icon + 徽章名 + 获得时间，3 秒自动消失)
  │   如果同时获得多枚 → 逐个弹出，每枚 3 秒
  │
  └─ POST /achievement/new/ack (确认已读)
```

---

## 八、文件清单

```
recite-v2/
│
├── recite-api/src/main/java/cn/bugstack/recite/api/
│   ├── IAchievementService.java
│   └── dto/
│       ├── BadgeDTO.java
│       ├── BadgeDetailDTO.java
│       └── NewBadgeDTO.java
│
├── recite-domain/src/main/java/cn/bugstack/recite/domain/achievement/
│   ├── model/entity/
│   │   └── AchievementLog.java
│   ├── model/valueobj/
│   │   ├── BadgeDefinition.java
│   │   └── UserStatsVO.java
│   ├── model/event/
│   │   └── AchievementRequestMessage.java
│   ├── port/out/
│   │   ├── AchievementPort.java
│   │   ├── NewBadgePort.java
│   ├── service/
│   │   ├── BadgeRegistry.java
│   │   └── AchievementService.java
│   └── exception/
│       └── AchievementException.java
│
├── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/adapter/
│   ├── persistence/
│   │   ├── AchievementLogDO.java
│   │   ├── AchievementLogMapper.java
│   │   └── AchievementPersistenceAdapter.java
│   ├── cache/
│   │   └── NewBadgeRedisAdapter.java
│   └── mq/
│       └── AchievementConsumer.java
│
└── recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/
    └── AchievementController.java
```

**总计 ~20 个文件**（4 api + 11 domain + 4 infra + 1 trigger）

---

## 九、编码顺序

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `AchievementException.java` | 其他类依赖 |
| 2 | `AchievementLog.java` `AchievementRequestMessage.java` | 数据结构 |
| 3 | `BadgeDefinition.java` `UserStatsVO.java` | 值对象 |
| 4 | `BadgeRegistry.java` | 46 枚硬编码定义 |
| 5 | 5 个 Port 接口 | 领域契约 |
| 6 | `AchievementService.java` | 评估 + 进度计算 |
| 7 | DTO（3个）+ `IAchievementService.java` | API 契约 |
| 8 | DO + Mapper + `AchievementPersistenceAdapter` | 持久层 |
| 9 | `NewBadgeRedisAdapter.java` | Redis 操作 |
| 10 | `AchievementConsumer.java` | MQ 消费者 |
| 11 | `AchievementController.java` | 控制器 |

每步完成后 `mvn compile` 验证。
