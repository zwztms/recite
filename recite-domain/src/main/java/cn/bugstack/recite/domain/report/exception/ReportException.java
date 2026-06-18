package cn.bugstack.recite.domain.report.exception;

import cn.bugstack.recite.types.exception.AppException;

/**
 * report 子域异常.
 * <p>场景：报告未生成、报告不存在、仪表盘数据为空.</p>
 */
public class ReportException extends AppException {

    public ReportException(String message) {
        super("500", message);
    }

    public ReportException(String code, String message) {
        super(code, message);
    }
}
