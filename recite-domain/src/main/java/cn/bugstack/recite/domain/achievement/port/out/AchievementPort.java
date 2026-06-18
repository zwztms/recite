package cn.bugstack.recite.domain.achievement.port.out;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 徽章记录持久化 SPI.
 */
public interface AchievementPort {

    /** 保存徽章获得记录 */
    void save(Long userId, String badgeKey, LocalDateTime earnedAt);

    /** 查用户已获得的所有徽章 key */
    List<String> findEarnedBadgeKeys(Long userId);

    /** 用户累计获得徽章数 */
    int countByUserId(Long userId);

    /** 查用户已获得徽章 key → 获得时间 */
    java.util.Map<String, LocalDateTime> findEarnedBadgeMap(Long userId);
}
