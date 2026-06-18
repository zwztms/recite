package cn.bugstack.recite.domain.recite.port.out;

/**
 * 成就消息 SPI — 发 MQ 触发异步徽章评估.
 */
public interface AchievementMessagePort {

    /** 发送成就评估请求到 MQ，fire-and-forget */
    void sendAchievementRequest(Long userId, String sessionId);
}
