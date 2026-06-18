package cn.bugstack.recite.infrastructure.adapter.mq;

import cn.bugstack.recite.domain.achievement.model.event.AchievementRequestMessage;
import cn.bugstack.recite.domain.recite.port.out.AchievementMessagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

/**
 * 成就消息适配器 — RocketMQ 实现，fire-and-forget.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementMessageAdapter implements AchievementMessagePort {

    private static final String TOPIC = "recite-achievement-topic";

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void sendAchievementRequest(Long userId, String sessionId) {
        AchievementRequestMessage msg = new AchievementRequestMessage(userId, sessionId);
        rocketMQTemplate.convertAndSend(TOPIC, msg);
        log.info("成就评估请求已发送: userId={}, sessionId={}", userId, sessionId);
    }
}
