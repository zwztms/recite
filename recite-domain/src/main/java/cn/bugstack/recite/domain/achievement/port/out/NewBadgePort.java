package cn.bugstack.recite.domain.achievement.port.out;

import java.util.List;

/**
 * 新徽章 Redis 操作 SPI — 前端轮询用.
 */
public interface NewBadgePort {

    /** 写入待领取徽章集合，设置 5 分钟 TTL */
    void addNewBadges(Long userId, List<String> badgeKeys);

    /** 读取待领取徽章列表 */
    List<String> getNewBadges(Long userId);

    /** 确认已读，删除 Key */
    void ackNewBadges(Long userId);
}
