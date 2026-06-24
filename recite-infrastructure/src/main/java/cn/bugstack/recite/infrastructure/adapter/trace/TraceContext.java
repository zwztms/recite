package cn.bugstack.recite.infrastructure.adapter.trace;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 链路追踪上下文 — ThreadLocal 存储当前请求的 traceId 和节点调用栈.
 * 与 {@link cn.bugstack.recite.trigger.config.UserContext} 职责分离：
 * UserContext 存用户身份，TraceContext 存追踪标识.
 */
public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Deque<Long>> NODE_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private TraceContext() {}

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    /** 快照当前调用栈深度和父节点 ID，随后节点 ID 入栈（调用 insertNode 获得 id 后） */
    public static NodeContext pushNode() {
        Deque<Long> stack = NODE_STACK.get();
        NodeContext ctx = new NodeContext(stack.size(), stack.peekLast());
        stack.addLast(null); // 占位，等 insertNode 后 setNodeId 更新
        return ctx;
    }

    /** 更新栈顶占位为真实节点 ID（insertNode 后调用） */
    public static void setNodeId(long nodeId) {
        Deque<Long> stack = NODE_STACK.get();
        if (!stack.isEmpty()) {
            stack.removeLast();
            stack.addLast(nodeId);
        }
    }

    /** 当前节点出栈 */
    public static void popNode() {
        Deque<Long> stack = NODE_STACK.get();
        if (!stack.isEmpty()) stack.removeLast();
    }

    public static void clear() {
        TRACE_ID.remove();
        NODE_STACK.get().clear();
    }

    public record NodeContext(int depth, Long parentNodeId) {}
}

