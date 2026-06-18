# Phase 8 编码报告 — achievement 成就子域

> 日期：2026-06-18 | 范围：46 枚徽章定义 + MQ 异步评估 + 徽章墙 + 前端轮询 | 编译：BUILD SUCCESS

---

## 一、文件清单

### recite-domain/achievement（11 个）

| 文件 | 职责 |
|------|------|
| `AchievementException.java` | 子域异常，继承 AppException |
| `AchievementLog.java` | 徽章获得记录实体：id, userId, badgeKey, earnedAt |
| `BadgeDefinition.java` | 徽章定义值对象：key/name/description/icon/category/hidden/condition 函数 |
| `BadgeProgress.java` | 进度值对象：current/target/percent |
| `UserStatsVO.java` | 用户统计快照（14 字段），传给 condition 评估 |
| `AchievementRequestMessage.java` | MQ 消息体：userId, sessionId（Serializable） |
| `AchievementPort.java` | SPI：save / findEarnedBadgeKeys / countByUserId / findEarnedBadgeMap |
| `NewBadgePort.java` | SPI：addNewBadges / getNewBadges / ackNewBadges（Redis 操作） |
| `BadgeRegistry.java` | 46 枚徽章硬编码定义（final class，constructor 禁止实例化） |
| `AchievementService.java` | 领域服务：evaluateAll() 评估编排 + calculateProgress() 进度计算 |

### recite-api（4 个）

| 文件 | 职责 |
|------|------|
| `IAchievementService.java` | 4 端点 REST 契约：listAll / getDetail / getNewBadges / ackNewBadges |
| `BadgeDTO.java` | 徽章卡片：含 earned/earnedAt/progress 嵌套类 |
| `BadgeDetailDTO.java` | 徽章详情：含 earnCondition/detailedDescription |
| `NewBadgeDTO.java` | 新徽章轮询返回：key/name/description/icon |

### recite-infrastructure（5 个）

| 文件 | 职责 |
|------|------|
| `AchievementLogDO.java` | @TableName("achievement_log")，映射 PG 表 |
| `AchievementLogMapper.java` | extends BaseMapper\<AchievementLogDO\> |
| `AchievementPersistenceAdapter.java` | 实现 AchievementPort，MyBatis Plus |
| `NewBadgeRedisAdapter.java` | 实现 NewBadgePort，Redis SMEMBERS/SADD/DEL，TTL 5min |
| `AchievementMessageAdapter.java` | 实现 AchievementMessagePort，RocketMQ 发送 fire-and-forget |
| `AchievementConsumer.java` | @RocketMQMessageListener，topic=recite-achievement-topic，查统计→评估→写 PG→写 Redis |

### recite-trigger（1 个）

| 文件 | 职责 |
|------|------|
| `AchievementController.java` | 实现 IAchievementService，DTO 映射 + 进度计算 |

### 配合改动（3 个）

| 文件 | 改动 |
|------|------|
| `AchievementMessagePort.java`（recite-domain，新建）| SPI：sendAchievementRequest(userId, sessionId) |
| `ReciteOrchestrationService.java` | finishRecite：注入 AchievementMessagePort，发 MQ 异步评估徽章 |
| `AchievementPort.java`（追加）| 加 findEarnedBadgeMap() 方法 |
| `AchievementPersistenceAdapter.java`（追加）| 实现 findEarnedBadgeMap() |

**总计 22 个新文件 + 3 个修改**。

---

## 二、异步徽章评估调用链路

```
POST /recite/{sid}/finish
  ReciteOrchestrationService.finishRecite(userId, sid)
    ├─ Java 基础统计 + 更新间隔重复 + checkIn
    ├─ reportMessagePort.sendReportRequest() → MQ 异步报告
    ├─ achievementMessagePort.sendAchievementRequest() → MQ 异步评估（新增）
    ├─ session.status = FINISHED
    └─ return SessionReportVO


       ═══════════ MQ 异步边界 ═══════════


AchievementConsumer.onMessage(msg)
  ├─ ① 查全量统计数据
  │    ReciteRecordPort: countByUserId / avgScoreByUserId / countPerfectScores
  │                     / countSessionsByUserId / findBySessionId
  │    ProgressPort: countMastered
  │    StreakPort: findByUserId → currentStreak / longestStreak
  │    AchievementPort: findEarnedBadgeKeys → 已获得列表
  │    → 组装 UserStatsVO（14 字段，含会话级隐藏徽章字段）
  │
  ├─ ② achievementService.evaluateAll(stats)
  │    逐枚遍历 46 枚 BadgeDefinition
  │    跳过已获得 → condition.evaluate(stats) → 新徽章列表
  │
  ├─ ③ 逐枚 achievementPort.save(userId, badgeKey, now)
  │    INSERT INTO achievement_log (user_id, badge_key, earned_at)
  │    UNIQUE(user_id, badge_key) 防止重复发放
  │
  └─ ④ newBadgePort.addNewBadges(userId, newKeys)
       SADD recite:new_badges:{userId}  badgeKey1 badgeKey2 ...
       EXPIRE recite:new_badges:{userId} 300 (5 分钟)


       ═══════════ 前端轮询 ═══════════


finishRecite 返回后，前端每 2 秒轮询:
  GET /achievement/new
    └─ NewBadgePort.getNewBadges(userId)
         └─ SMEMBERS recite:new_badges:{userId}
              ├─ [] → 继续轮询（消费者可能还在评估）
              └─ [{key:"total_100", name:"百题斩", ...}] → 弹 Toast

  弹 Toast 后:
    POST /achievement/new/ack
      └─ DEL recite:new_badges:{userId}（不再重复弹）
```

---

## 三、徽章墙调用链路

```
GET /achievement/
  AchievementController.listAll()
    ├─ ① achievementPort.findEarnedBadgeMap(userId)
    │     → Map<badgeKey, earnedAt>
    │
    ├─ ② 查用户进度统计
    │     ReciteRecordPort: countByUserId / avgScoreByUserId / etc.
    │     ProgressPort: countMastered
    │     StreakPort: findByUserId
    │     → buildLightStats() → UserStatsVO（不含会话字段）
    │
    ├─ ③ 逐模块 countByModule（仅未获得模块徽章的才查，最多 19 次）
    │
    ├─ ④ 遍历全部公开徽章（38 枚）
    │     每枚组装 BadgeDTO {earned, earnedAt, progress}
    │     achievementService.calculateProgress(badge, stats, moduleCounts)
    │       ├─ 背诵量: totalRecites / 10|50|100|300|500
    │       ├─ 质量: averageScore / 7|8|9, perfectScoreCount / 1|10
    │       ├─ 坚持: currentStreak / 3|7|14|30|60
    │       ├─ 模块: countByModule / 20
    │       └─ 组合: 已获得所需模块数 / 总需模块数
    │
    └─ 返回 List<BadgeDTO>（前端按 category 分组卡片网格）
```

---

## 四、46 枚徽章分类

| 类别 | 数量 | 示例 | 隐藏 |
|------|:--:|------|:--:|
| 背诵量 | 5 | 初出茅庐(10)→题库终结者(500) | |
| 质量 | 5 | 稳定发挥(avg≥7)→满分收割机(10次10分) | |
| 坚持 | 5 | 三天打鱼(3天)→持之以恒(60天) | |
| 模块单枚 | 19 | Java 基础达人…Agent 专家（每模块≥20题） | |
| 组合 | 4 | Java全栈/AI专家/计算机基础/全模块制霸 | |
| 趣味隐藏 | 8 | 夜猫子/快枪手/三星上将/卷土重来/第一滴血/马拉松/狙击手/收藏家 | ✅ |

隐藏徽章在徽章墙上不展示（仅在已获得时可见），详情页条件显示 "???"。

---

## 五、新增数据库表

```sql
CREATE TABLE IF NOT EXISTS achievement_log (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users(id),
    badge_key VARCHAR(64) NOT NULL,
    earned_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, badge_key)
);
CREATE INDEX IF NOT EXISTS idx_al_user ON achievement_log(user_id);
```

---

## 六、新增 Redis Key

| Key | 类型 | 用途 | TTL |
|------|:--:|------|:--:|
| `recite:new_badges:{userId}` | SET | 待领取徽章列表 | 300s |

---

## 七、新增 RocketMQ Topic

| Topic | 消费者 | 触发时机 |
|------|------|------|
| `recite-achievement-topic` | AchievementConsumer | finishRecite 后 |

---

## 八、进度计算策略

| 徽章类型 | current | target | 示例 |
|------|------|:--:|------|
| `total_*` | totalRecites | key 后缀数字 | 当前 67 题，百题斩需要 100 → 67% |
| `avg_*` | averageScore × 10 | key 后缀 × 10 | 当前均分 6.8，avg_7 需要 7.0 → 97% |
| `perfect_*` | perfectScoreCount | key 后缀数字 | 当前 3 次，满分收割机需要 10 → 30% |
| `streak_*` | currentStreak | key 后缀数字 | 当前 5 天，周不懈怠需要 7 → 71% |
| 模块 | countByModule | 20 | 12/20 → 60% |
| `combo_*` | 已获得所需模块数 | 总需模块数 | 2/4 Java模块 → 50% |
| `hidden_*` | — | — | 不展示进度 |

---

## 九、跨子域只读依赖

AchievementConsumer 和 AchievementController 均以只读方式复用其他子域的 Port：

| Port | 来源 | 方法 |
|------|------|------|
| `ReciteRecordPort` | Phase 5 recite | countByUserId / avgScoreByUserId / countPerfectScores / countSessionsByUserId / countByModule / findBySessionId |
| `ProgressPort` | Phase 6 progress | countMastered |
| `StreakPort` | Phase 6 progress | findByUserId |

---

## 十、编译结果

```
recite-types          SUCCESS
recite-api            SUCCESS
recite-domain         SUCCESS
recite-infrastructure SUCCESS
recite-trigger        SUCCESS
recite-app            SUCCESS
BUILD SUCCESS
```

---

## 十一、提交记录

| Commit | 说明 |
|------|------|
| `705f94c` | phase8-1: 领域模型 + Port 接口（8 文件） |
| `6a9c285` | phase8-2: BadgeRegistry（46枚）+ AchievementService 评估与进度 |
| `c45456b` | phase8-3: API 契约 — IAchievementService + 3 DTO |
| `a7bd7e2` | phase8-4: 持久层 — AchievementLogDO + Mapper + PersistenceAdapter |
| `dcc4669` | phase8-5: NewBadgeRedisAdapter + AchievementMessageAdapter + finishRecite 埋点 |
| `8e8689d` | phase8-6: AchievementConsumer MQ 消费者 — 异步评估 46 枚徽章 |
| `56b1910` | phase8-7: AchievementController 4 端点 — 徽章墙/详情/轮询/确认 |
