package cn.bugstack.recite.infrastructure.adapter.mq;

import cn.bugstack.recite.domain.report.model.event.ReportRequestMessage;
import cn.bugstack.recite.domain.recite.port.out.ReportMessagePort;
import cn.bugstack.recite.types.annotation.ReciteTraceNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 报告消息适配器 — RocketMQ 实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMessageAdapter implements ReportMessagePort {

    private static final String TOPIC = "recite-report-topic";

    private final RocketMQTemplate rocketMQTemplate;

    @ReciteTraceNode(type = "MQ", name = "发送报告消息")
    @Override
    public void sendReportRequest(Long userId, String sessionId, List<Long> recordIds) {
        ReportRequestMessage msg = new ReportRequestMessage(userId, sessionId, recordIds);
        rocketMQTemplate.convertAndSend(TOPIC, msg);
        log.info("报告生成请求已发送: userId={}, sessionId={}, records={}", userId, sessionId, recordIds.size());
    }
}
