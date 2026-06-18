package cn.bugstack.recite.domain.report.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * MQ 报告请求消息体 — 仅数据，无行为.
 *
 * <p>由 recite 子域发送到 RocketMQ Topic "recite-report-topic",
 * report 子域的 ReportGenerateConsumer 消费.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestMessage implements Serializable {

    private Long userId;
    private String sessionId;
    private List<Long> recordIds;
}
