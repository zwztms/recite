package cn.bugstack.recite.domain.recite.exception;

import cn.bugstack.recite.types.exception.AppException;

/**
 * recite 子域异常.
 * <p>场景：会话不存在/已过期、用户越权、评分槽位满、追问超层.</p>
 */
public class ReciteException extends AppException {

    public ReciteException(String code, String message) {
        super(code, message);
    }

    public ReciteException(String message) {
        super("500", message);
    }
}
