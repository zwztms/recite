package cn.bugstack.recite.domain.auth.exception;

import cn.bugstack.recite.types.exception.AppException;

/**
 * auth 认证异常.
 */
public class AuthException extends AppException {

    public AuthException(String message) {
        super("401", message);
    }

    public AuthException(String code, String message) {
        super(code, message);
    }
}
