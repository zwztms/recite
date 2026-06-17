package cn.bugstack.recite.domain.progress.exception;

import cn.bugstack.recite.types.exception.AppException;

/**
 * progress 子域异常.
 */
public class ProgressException extends AppException {

    public ProgressException(String message) {
        super("500", message);
    }
}
