package cn.bugstack.recite.trigger.config;

import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 — 将所有 AppException 及其子类统一转为 Response 格式.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response<Void> handleAppException(AppException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Response.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Response<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return Response.fail("500", "服务器内部错误");
    }
}
