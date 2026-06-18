package cn.bugstack.recite.domain.achievement.model.valueobj;

import java.util.Set;

/**
 * 用户统计快照值对象 — 传给 BadgeDefinition.condition 评估.
 *
 * <p>由 AchievementConsumer 在收到 MQ 消息后查询组装，涵盖背诵量/质量/
 * 坚持/模块/隐藏徽章所需的全部统计维度.</p>
 */
public class UserStatsVO {

    /** 累计背诵次数 */
    private final int totalRecites;
    /** 历史平均分 */
    private final double averageScore;
    /** 满分次数 (score=10) */
    private final int perfectScoreCount;
    /** 当前连续天数 */
    private final int currentStreak;
    /** 最长连续天数 */
    private final int longestStreak;
    /** masteryScore >= 80 的题数 */
    private final int masteredCount;
    /** 完成会话次数 */
    private final int totalSessions;
    /** 已获得模块徽章的 moduleKey */
    private final Set<String> earnedModuleBadges;
    /** 已获得全部徽章的 key */
    private final Set<String> earnedBadgeKeys;
    /** 上次会话的小时 (0-23)，隐藏徽章用 */
    private final int lastSessionHour;
    /** 上次会话最快答题秒数，隐藏徽章用 */
    private final int lastSessionAnswerSeconds;
    /** 上次会话满分题数，隐藏徽章用 */
    private final int lastSessionPerfectCount;
    /** 上次会话题数，隐藏徽章用 */
    private final int lastSessionQuestionCount;
    /** 上次会话最大追问深度，隐藏徽章用 */
    private final int lastSessionMaxFollowUpDepth;
    /** 是否曾经断签 >=7 天后恢复，隐藏徽章用 */
    private final boolean wasStreakBroken;

    public UserStatsVO(int totalRecites, double averageScore, int perfectScoreCount,
                       int currentStreak, int longestStreak, int masteredCount,
                       int totalSessions, Set<String> earnedModuleBadges,
                       Set<String> earnedBadgeKeys, int lastSessionHour,
                       int lastSessionAnswerSeconds, int lastSessionPerfectCount,
                       int lastSessionQuestionCount, int lastSessionMaxFollowUpDepth,
                       boolean wasStreakBroken) {
        this.totalRecites = totalRecites;
        this.averageScore = averageScore;
        this.perfectScoreCount = perfectScoreCount;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.masteredCount = masteredCount;
        this.totalSessions = totalSessions;
        this.earnedModuleBadges = earnedModuleBadges;
        this.earnedBadgeKeys = earnedBadgeKeys;
        this.lastSessionHour = lastSessionHour;
        this.lastSessionAnswerSeconds = lastSessionAnswerSeconds;
        this.lastSessionPerfectCount = lastSessionPerfectCount;
        this.lastSessionQuestionCount = lastSessionQuestionCount;
        this.lastSessionMaxFollowUpDepth = lastSessionMaxFollowUpDepth;
        this.wasStreakBroken = wasStreakBroken;
    }

    // ---- getters ----

    public int getTotalRecites() { return totalRecites; }
    public double getAverageScore() { return averageScore; }
    public int getPerfectScoreCount() { return perfectScoreCount; }
    public int getCurrentStreak() { return currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public int getMasteredCount() { return masteredCount; }
    public int getTotalSessions() { return totalSessions; }
    public Set<String> getEarnedModuleBadges() { return earnedModuleBadges; }
    public Set<String> getEarnedBadgeKeys() { return earnedBadgeKeys; }
    public int getLastSessionHour() { return lastSessionHour; }
    public int getLastSessionAnswerSeconds() { return lastSessionAnswerSeconds; }
    public int getLastSessionPerfectCount() { return lastSessionPerfectCount; }
    public int getLastSessionQuestionCount() { return lastSessionQuestionCount; }
    public int getLastSessionMaxFollowUpDepth() { return lastSessionMaxFollowUpDepth; }
    public boolean isWasStreakBroken() { return wasStreakBroken; }
}
