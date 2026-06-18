package cn.bugstack.recite.infrastructure.adapter.trace;

/**
 * 链路追踪上下文 — ThreadLocal 存储当前请求的 traceId.
 * 与 {@link cn.bugstack.recite.trigger.config.UserContext} 职责分离：
 * UserContext 存用户身份，TraceContext 存追踪标识.
 */
public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {}

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
