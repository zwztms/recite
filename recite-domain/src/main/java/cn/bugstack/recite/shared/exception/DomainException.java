package cn.bugstack.recite.shared.exception;

import cn.bugstack.recite.types.enums.ResponseCode;

/**
 * 领域异常基类 — 所有子域异常由此派生。
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DomainException(ResponseCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public DomainException(ResponseCode rc, Throwable cause) {
        super(rc.getMessage(), cause);
        this.code = rc.getCode();
    }

    public String getCode() {
        return code;
    }
}
