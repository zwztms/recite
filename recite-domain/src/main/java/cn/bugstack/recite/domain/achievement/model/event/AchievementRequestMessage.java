package cn.bugstack.recite.domain.achievement.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * MQ 成就评估请求消息体 — 仅数据，无行为.
 *
 * <p>由 recite 子域的 finishRecite 发送到 RocketMQ Topic "recite-achievement-topic",
 * achievement 子域的 AchievementConsumer 消费.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementRequestMessage implements Serializable {

    private Long userId;
    private String sessionId;
}
