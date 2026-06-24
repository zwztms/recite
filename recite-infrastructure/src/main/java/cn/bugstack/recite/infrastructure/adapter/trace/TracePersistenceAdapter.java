package cn.bugstack.recite.infrastructure.adapter.trace;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 链路追踪持久层适配器 — 封装 trace_runs / trace_nodes 的 CRUD.
 */
@Component
public class TracePersistenceAdapter {

    @Resource
    private TraceRunMapper traceRunMapper;

    @Resource
    private TraceNodeMapper traceNodeMapper;

    public void insertRun(TraceRunDO run) {
        traceRunMapper.insert(run);
    }

    public void updateRunSuccess(String traceId, long latencyMs) {
        traceRunMapper.update(null, new LambdaUpdateWrapper<TraceRunDO>()
                .eq(TraceRunDO::getTraceId, traceId)
                .set(TraceRunDO::getStatus, "SUCCESS")
                .set(TraceRunDO::getLatencyMs, latencyMs));
    }

    public void updateRunError(String traceId, long latencyMs, String errorMsg) {
        traceRunMapper.update(null, new LambdaUpdateWrapper<TraceRunDO>()
                .eq(TraceRunDO::getTraceId, traceId)
                .set(TraceRunDO::getStatus, "ERROR")
                .set(TraceRunDO::getLatencyMs, latencyMs)
                .set(TraceRunDO::getErrorMsg, errorMsg));
    }

    public void insertNode(TraceNodeDO node) {
        traceNodeMapper.insert(node);
    }

    public void updateNodeSuccess(Long id, long latencyMs) {
        traceNodeMapper.update(null, new LambdaUpdateWrapper<TraceNodeDO>()
                .eq(TraceNodeDO::getId, id)
                .set(TraceNodeDO::getStatus, "SUCCESS")
                .set(TraceNodeDO::getLatencyMs, latencyMs));
    }

    public void updateNodeError(Long id, long latencyMs) {
        traceNodeMapper.update(null, new LambdaUpdateWrapper<TraceNodeDO>()
                .eq(TraceNodeDO::getId, id)
                .set(TraceNodeDO::getStatus, "ERROR")
                .set(TraceNodeDO::getLatencyMs, latencyMs));
    }

    /** 更新节点的阶段摘要 extraData（在 updateNodeSuccess 之后调用） */
    public void updateNodeExtra(Long id, String extraData) {
        traceNodeMapper.update(null, new LambdaUpdateWrapper<TraceNodeDO>()
                .eq(TraceNodeDO::getId, id)
                .set(TraceNodeDO::getExtraData, extraData));
    }
}
