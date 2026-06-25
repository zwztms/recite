package cn.bugstack.recite.infrastructure.adapter.mq;

import cn.bugstack.recite.domain.achievement.model.event.AchievementRequestMessage;
import cn.bugstack.recite.domain.recite.port.out.AchievementMessagePort;
import cn.bugstack.recite.types.annotation.ReciteTraceNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

/**
 * 成就消息适配器 — RocketMQ 异步发送.
 * <p>
 * {@link SendCallback} 回调在 Netty IO 线程执行，
 * 只做日志输出，不做任何阻塞操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementMessageAdapter implements AchievementMessagePort {

    private static final String TOPIC = "recite-achievement-topic";

    private final RocketMQTemplate rocketMQTemplate;

    @ReciteTraceNode(type = "MQ", name = "发送成就消息")
    @Override
    public void sendAchievementRequest(Long userId, String sessionId) {
        AchievementRequestMessage msg = new AchievementRequestMessage(userId, sessionId);
        rocketMQTemplate.asyncSend(TOPIC, msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult result) {
                log.info("成就消息发送成功: userId={}, sid={}, msgId={}", userId, sessionId, result.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.warn("成就消息发送失败: userId={}, sid={}, err={}", userId, sessionId, e.getMessage());
            }
        });
    }
}
