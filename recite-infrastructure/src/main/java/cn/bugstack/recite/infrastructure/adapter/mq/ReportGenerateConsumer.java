package cn.bugstack.recite.infrastructure.adapter.mq;

import cn.bugstack.recite.domain.report.model.event.ReportRequestMessage;
import cn.bugstack.recite.domain.report.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 报告生成消费者 — 消费 recite-report-topic，异步生成学习档案.
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "recite-report-topic",
        consumerGroup = "recite-report-consumer"
)
public class ReportGenerateConsumer implements RocketMQListener<ReportRequestMessage> {

    private final ReportService reportService;

    public ReportGenerateConsumer(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public void onMessage(ReportRequestMessage msg) {
        log.info("收到报告生成请求: userId={}, sessionId={}", msg.getUserId(), msg.getSessionId());
        try {
            reportService.generateReport(msg.getUserId(), msg.getSessionId());
        } catch (Exception e) {
            log.error("报告生成失败: userId={}, sessionId={}", msg.getUserId(), msg.getSessionId(), e);
            throw e; // RocketMQ 自动重试（默认 16 次）
        }
    }
}
