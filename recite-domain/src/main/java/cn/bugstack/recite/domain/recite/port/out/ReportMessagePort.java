package cn.bugstack.recite.domain.recite.port.out;

import java.util.List;

/**
 * 报告消息 SPI — 发 MQ 触发异步报告生成.
 */
public interface ReportMessagePort {

    /** 发送报告生成请求到 MQ，fire-and-forget */
    void sendReportRequest(Long userId, String sessionId, List<Long> recordIds);
}
