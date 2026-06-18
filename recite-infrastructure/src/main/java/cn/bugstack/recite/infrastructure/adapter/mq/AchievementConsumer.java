package cn.bugstack.recite.infrastructure.adapter.mq;

import cn.bugstack.recite.domain.achievement.model.entity.AchievementLog;
import cn.bugstack.recite.domain.achievement.model.event.AchievementRequestMessage;
import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeDefinition;
import cn.bugstack.recite.domain.achievement.model.valueobj.UserStatsVO;
import cn.bugstack.recite.domain.achievement.port.out.AchievementPort;
import cn.bugstack.recite.domain.achievement.port.out.NewBadgePort;
import cn.bugstack.recite.domain.achievement.service.AchievementService;
import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成就评估消费者 — 消费 recite-achievement-topic，异步评估徽章.
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "recite-achievement-topic",
        consumerGroup = "recite-achievement-consumer"
)
public class AchievementConsumer implements RocketMQListener<AchievementRequestMessage> {

    private static final Set<String> ALL_MODULE_KEYS = Set.of(
            "java-basics", "juc", "jvm", "java-collections", "spring", "mysql", "redis",
            "os", "ds-algo", "network",
            "ai-rag", "ai-spring", "ai-finetune", "ai-prompt", "ai-eval", "ai-security",
            "ai-design", "ai-openclaw", "ai-agent"
    );

    private final ReciteRecordPort reciteRecordPort;
    private final ProgressPort progressPort;
    private final StreakPort streakPort;
    private final AchievementPort achievementPort;
    private final NewBadgePort newBadgePort;
    private final AchievementService achievementService;

    public AchievementConsumer(ReciteRecordPort reciteRecordPort,
                               ProgressPort progressPort,
                               StreakPort streakPort,
                               AchievementPort achievementPort,
                               NewBadgePort newBadgePort,
                               AchievementService achievementService) {
        this.reciteRecordPort = reciteRecordPort;
        this.progressPort = progressPort;
        this.streakPort = streakPort;
        this.achievementPort = achievementPort;
        this.newBadgePort = newBadgePort;
        this.achievementService = achievementService;
    }

    @Override
    public void onMessage(AchievementRequestMessage msg) {
        Long userId = msg.getUserId();
        String sessionId = msg.getSessionId();
        log.info("收到成就评估请求: userId={}, sessionId={}", userId, sessionId);

        try {
            // ① 查全量统计数据
            UserStatsVO stats = buildStats(userId, sessionId);

            // ② 逐枚评估
            List<BadgeDefinition> newlyEarned = achievementService.evaluateAll(stats);
            if (newlyEarned.isEmpty()) {
                log.info("无新徽章: userId={}", userId);
                return;
            }

            // ③ 逐枚写入 achievement_log
            LocalDateTime now = LocalDateTime.now();
            List<String> newKeys = newlyEarned.stream().map(BadgeDefinition::getKey).toList();
            for (BadgeDefinition badge : newlyEarned) {
                achievementPort.save(userId, badge.getKey(), now);
            }
            log.info("新徽章已写入: userId={}, keys={}", userId, newKeys);

            // ④ 标记新徽章（Redis）
            newBadgePort.addNewBadges(userId, newKeys);
        } catch (Exception e) {
            log.error("成就评估失败: userId={}, sessionId={}", userId, sessionId, e);
            throw e; // RocketMQ 自动重试
        }
    }

    /** 组装用户统计快照 */
    private UserStatsVO buildStats(Long userId, String sessionId) {
        // 宏观统计
        int totalRecites = reciteRecordPort.countByUserId(userId);
        double avgScore = reciteRecordPort.avgScoreByUserId(userId);
        int perfectScoreCount = reciteRecordPort.countPerfectScores(userId);
        int totalSessions = reciteRecordPort.countSessionsByUserId(userId);
        int masteredCount = progressPort.countMastered(userId);

        // 连续天数
        Optional<UserStreakEntity> streakOpt = streakPort.findByUserId(userId);
        int currentStreak = streakOpt.map(UserStreakEntity::getCurrentStreak).orElse(0);
        int longestStreak = streakOpt.map(UserStreakEntity::getLongestStreak).orElse(0);

        // 已获得徽章
        List<String> earnedBadgeKeys = achievementPort.findEarnedBadgeKeys(userId);
        Set<String> earnedSet = new HashSet<>(earnedBadgeKeys);
        Set<String> earnedModules = earnedSet.stream()
                .filter(ALL_MODULE_KEYS::contains)
                .collect(Collectors.toSet());

        // 本次会话记录
        List<ReciteRecordEntity> sessionRecords = reciteRecordPort.findBySessionId(userId, sessionId);
        int lastSessionQuestionCount = sessionRecords.size();
        int lastSessionPerfectCount = (int) sessionRecords.stream()
                .filter(r -> r.getScore() != null && r.getScore() == 10).count();
        int lastSessionMaxFollowUpDepth = sessionRecords.stream()
                .mapToInt(ReciteRecordEntity::getFollowUpDepth).max().orElse(0);
        int lastSessionAnswerSeconds = sessionRecords.stream()
                .map(ReciteRecordEntity::getResponseTimeSeconds)
                .filter(t -> t != null && t > 0)
                .min(Integer::compareTo).orElse(Integer.MAX_VALUE);

        // 会话时间（取首条记录创建时间的小时，否则用当前时间）
        int lastSessionHour = sessionRecords.stream().findFirst()
                .map(r -> r.getCreatedAt())
                .filter(dt -> dt != null)
                .map(LocalDateTime::getHour)
                .orElse(LocalDateTime.now().getHour());

        // 是否曾断签（最长 > 当前 意味着曾经有更长连续记录后中断）
        boolean wasStreakBroken = longestStreak > 0 && currentStreak < longestStreak;

        return new UserStatsVO(totalRecites, avgScore, perfectScoreCount,
                currentStreak, longestStreak, masteredCount, totalSessions,
                earnedModules, earnedSet, lastSessionHour,
                lastSessionAnswerSeconds, lastSessionPerfectCount,
                lastSessionQuestionCount, lastSessionMaxFollowUpDepth,
                wasStreakBroken);
    }
}
