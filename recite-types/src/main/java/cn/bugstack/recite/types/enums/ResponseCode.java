package cn.bugstack.recite.types.enums;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS("0", "成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未登录"),
    FORBIDDEN("403", "权限不足"),
    NOT_FOUND("404", "资源不存在"),
    SERVICE_ERROR("500", "服务内部错误"),

    // 业务
    QUESTION_NOT_FOUND("Q001", "题目不存在"),
    SESSION_NOT_FOUND("S001", "背诵会话不存在或已过期"),
    SESSION_EXPIRED("S002", "会话已超时"),
    LLM_TIMEOUT("L001", "AI 评分超时，请重试"),
    LLM_PARSE_ERROR("L002", "AI 返回格式异常"),
    ANSWER_EMPTY("A001", "答案不能为空"),
    ANSWER_TOO_LONG("A002", "答案不能超过 5000 字"),
    ;

    private final String code;
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
