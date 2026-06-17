package cn.bugstack.recite.trigger.config;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 用户上下文 — TTL ThreadLocal，支持父子线程传递.
 */
public final class UserContext {

    private static final ThreadLocal<Ctx> HOLDER = new TransmittableThreadLocal<>();

    private UserContext() {}

    public static void set(Long userId, String role) {
        HOLDER.set(new Ctx(userId, role));
    }

    public static Long getUserId() {
        Ctx ctx = HOLDER.get();
        return ctx != null ? ctx.userId : null;
    }

    public static String getRole() {
        Ctx ctx = HOLDER.get();
        return ctx != null ? ctx.role : null;
    }

    public static void clear() {
        HOLDER.remove();
    }

    private record Ctx(Long userId, String role) {}
}
