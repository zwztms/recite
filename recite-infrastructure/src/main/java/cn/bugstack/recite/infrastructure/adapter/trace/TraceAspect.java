package cn.bugstack.recite.infrastructure.adapter.trace;

import cn.bugstack.recite.types.annotation.ReciteTraceNode;
import cn.bugstack.recite.types.annotation.ReciteTraceRoot;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 链路追踪 AOP 切面.
 * <p>
 * 双切面：<br>
 * ① @ReciteTraceRoot — 生成 traceId，启停根记录 trace_runs，担保 TraceContext 清理<br>
 * ② @ReciteTraceNode — 记录环节明细 trace_nodes，无 trace 上下文时透传
 */
@Aspect
@Component
@Slf4j
public class TraceAspect {

    @Resource
    private TracePersistenceAdapter tracePersistenceAdapter;

    /** 切 @ReciteTraceRoot — 入口方法级，生成 traceId 并管理根记录生命周期 */
    @Around("@annotation(root)")
    public Object traceRoot(ProceedingJoinPoint pjp, ReciteTraceRoot root) throws Throwable {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        TraceContext.setTraceId(traceId);

        long start = System.currentTimeMillis();
        TraceRunDO run = new TraceRunDO();
        run.setTraceId(traceId);
        run.setEntryMethod(root.value());
        run.setStatus("RUNNING");
        run.setCreatedAt(LocalDateTime.now());
        tracePersistenceAdapter.insertRun(run);

        try {
            Object result = pjp.proceed();
            long latency = System.currentTimeMillis() - start;
            tracePersistenceAdapter.updateRunSuccess(traceId, latency);
            return result;
        } catch (Throwable e) {
            long latency = System.currentTimeMillis() - start;
            String msg = e.getMessage();
            if (msg != null && msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            tracePersistenceAdapter.updateRunError(traceId, latency, msg);
            throw e;
        } finally {
            TraceContext.clear();
        }
    }

    /** 切 @ReciteTraceNode — 内部环节级，记录单节点耗时，层次结构 + 阶段摘要 */
    @Around("@annotation(node)")
    public Object traceNode(ProceedingJoinPoint pjp, ReciteTraceNode node) throws Throwable {
        String traceId = TraceContext.getTraceId();
        if (traceId == null) {
            return pjp.proceed();
        }

        long start = System.currentTimeMillis();
        TraceContext.NodeContext ctx = TraceContext.pushNode();

        TraceNodeDO nodeDO = new TraceNodeDO();
        nodeDO.setTraceId(traceId);
        nodeDO.setNodeName(node.name());
        nodeDO.setNodeType(node.type());
        nodeDO.setDepth(ctx.depth());
        if (ctx.parentNodeId() != null) {
            nodeDO.setParentNodeId(String.valueOf(ctx.parentNodeId()));
        }
        nodeDO.setStatus("RUNNING");
        nodeDO.setCreatedAt(LocalDateTime.now());
        tracePersistenceAdapter.insertNode(nodeDO);
        TraceContext.setNodeId(nodeDO.getId());

        try {
            Object result = pjp.proceed();
            long latency = System.currentTimeMillis() - start;
            tracePersistenceAdapter.updateNodeSuccess(nodeDO.getId(), latency);
            writeExtraData(nodeDO.getId(), node.name(), result);
            return result;
        } catch (Throwable e) {
            long latency = System.currentTimeMillis() - start;
            tracePersistenceAdapter.updateNodeError(nodeDO.getId(), latency);
            throw e;
        } finally {
            TraceContext.popNode();
        }
    }

    /** 将阶段的输出摘要写入 extraData 字段 */
    private void writeExtraData(Long nodeId, String nodeName, Object result) {
        if (nodeName == null || result == null) return;
        try {
            String extra = null;
            String name = nodeName.toLowerCase();
            if (name.contains("queryrewriter") || name.contains("rewrite")) {
                extra = "{\"output\":\"" + truncate(result.toString(), 200) + "\"}";
            } else if (name.contains("intentrouter") || name.contains("router")) {
                extra = "{\"output\":\"" + truncate(result.toString(), 300) + "\"}";
            } else if (name.contains("multichannel") || name.contains("hybridsearch") || name.contains("retriev")) {
                extra = "{\"detail\":\"多通道检索完成\"}";
            } else if (name.contains("reranker") || name.contains("postprocess") || name.contains("rerank")) {
                extra = "{\"detail\":\"重排序完成\"}";
            } else if (name.contains("embedding") || name.contains("embed")) {
                extra = "{\"detail\":\"向量嵌入完成\"}";
            } else if (name.contains("score") || name.contains("评分")) {
                extra = "{\"output\":\"" + truncate(result.toString(), 100) + "\"}";
            }
            if (extra != null) {
                tracePersistenceAdapter.updateNodeExtra(nodeId, extra);
            }
        } catch (Exception ignored) {
            // extraData 写入失败不影响主流程
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
