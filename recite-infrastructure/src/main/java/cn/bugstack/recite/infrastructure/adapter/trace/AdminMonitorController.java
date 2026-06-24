package cn.bugstack.recite.infrastructure.adapter.trace;

import cn.bugstack.recite.api.response.Response;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 运维监控控制器 — 为 Phase 10 AdminMonitor.vue 提供链路追踪数据.
 * <p>
 * 放在 infrastructure 模块是因为：链路追踪是纯基础设施，不涉及业务领域.
 */
@Slf4j
@RestController
@RequestMapping("/admin/monitor")
@SaCheckRole("ADMIN")
public class AdminMonitorController {

    @Resource
    private TraceRunMapper traceRunMapper;

    @Resource
    private TraceNodeMapper traceNodeMapper;

    /** 分页查询链路追踪列表 */
    @GetMapping("/traces")
    public Response<PageResult<TraceRunVO>> listTraces(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TraceRunDO> p = new Page<>(page, size);
        traceRunMapper.selectPage(p, new LambdaQueryWrapper<TraceRunDO>()
                .orderByDesc(TraceRunDO::getCreatedAt));

        List<TraceRunVO> vos = p.getRecords().stream().map(r -> {
            TraceRunVO vo = new TraceRunVO();
            vo.setTraceId(r.getTraceId());
            vo.setUserId(r.getUserId());
            vo.setEntryMethod(r.getEntryMethod());
            vo.setStatus(r.getStatus());
            vo.setLatencyMs(r.getLatencyMs());
            vo.setErrorMsg(r.getErrorMsg());
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).toList();

        PageResult<TraceRunVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(p.getTotal());
        result.setPage(page);
        result.setSize(size);
        return Response.ok(result);
    }

    /** 查看单条链路的完整节点明细 */
    @GetMapping("/traces/{traceId}")
    public Response<TraceDetailVO> traceDetail(@PathVariable String traceId) {
        TraceRunDO run = traceRunMapper.selectOne(new LambdaQueryWrapper<TraceRunDO>()
                .eq(TraceRunDO::getTraceId, traceId));

        if (run == null) {
            return Response.fail("404", "链路不存在");
        }

        List<TraceNodeDO> nodeDOs = traceNodeMapper.selectByTraceId(traceId);
        List<TraceNodeVO> nodes = nodeDOs.stream().map(n -> {
            TraceNodeVO vo = new TraceNodeVO();
            vo.setNodeName(n.getNodeName());
            vo.setNodeType(n.getNodeType());
            vo.setStatus(n.getStatus());
            vo.setLatencyMs(n.getLatencyMs());
            vo.setExtraData(n.getExtraData());
            vo.setDepth(n.getDepth());
            vo.setParentNodeId(n.getParentNodeId());
            vo.setCreatedAt(n.getCreatedAt());
            return vo;
        }).toList();

        TraceRunVO runVO = new TraceRunVO();
        runVO.setTraceId(run.getTraceId());
        runVO.setUserId(run.getUserId());
        runVO.setEntryMethod(run.getEntryMethod());
        runVO.setStatus(run.getStatus());
        runVO.setLatencyMs(run.getLatencyMs());
        runVO.setErrorMsg(run.getErrorMsg());
        runVO.setCreatedAt(run.getCreatedAt());

        TraceDetailVO detail = new TraceDetailVO();
        detail.setTrace(runVO);
        detail.setNodes(nodes);
        return Response.ok(detail);
    }

    /** 带筛选的链路列表（按 status/userId 筛选） */
    @GetMapping("/traces/filter")
    public Response<PageResult<TraceRunVO>> listTracesFiltered(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {

        LambdaQueryWrapper<TraceRunDO> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) qw.eq(TraceRunDO::getStatus, status);
        if (userId != null) qw.eq(TraceRunDO::getUserId, userId);
        qw.orderByDesc(TraceRunDO::getCreatedAt);

        Page<TraceRunDO> p = new Page<>(page, size);
        traceRunMapper.selectPage(p, qw);

        List<TraceRunVO> vos = p.getRecords().stream().map(r -> {
            TraceRunVO vo = new TraceRunVO();
            vo.setTraceId(r.getTraceId());
            vo.setUserId(r.getUserId());
            vo.setEntryMethod(r.getEntryMethod());
            vo.setStatus(r.getStatus());
            vo.setLatencyMs(r.getLatencyMs());
            vo.setErrorMsg(r.getErrorMsg());
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).toList();

        PageResult<TraceRunVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(p.getTotal());
        result.setPage(page);
        result.setSize(size);
        return Response.ok(result);
    }

    /** 树形节点结构（按 depth + parentNodeId 展示层级） */
    @GetMapping("/traces/{traceId}/tree")
    public Response<List<TraceNodeVO>> traceTree(@PathVariable String traceId) {
        List<TraceNodeDO> nodeDOs = traceNodeMapper.selectByTraceId(traceId);
        List<TraceNodeVO> nodes = nodeDOs.stream().map(n -> {
            TraceNodeVO vo = new TraceNodeVO();
            vo.setNodeName(n.getNodeName());
            vo.setNodeType(n.getNodeType());
            vo.setStatus(n.getStatus());
            vo.setLatencyMs(n.getLatencyMs());
            vo.setExtraData(n.getExtraData());
            vo.setDepth(n.getDepth());
            vo.setParentNodeId(n.getParentNodeId());
            vo.setCreatedAt(n.getCreatedAt());
            return vo;
        }).toList();
        return Response.ok(nodes);
    }

    /** 今日统计：总请求数 / 平均耗时 / 异常数 */
    @GetMapping("/stats")
    public Response<Map<String, Object>> stats() {
        return Response.ok(traceRunMapper.selectTodayStats());
    }

    /** 清理指定天数之前的链路数据 */
    @DeleteMapping("/traces")
    public Response<Map<String, Integer>> cleanTraces(
            @RequestParam(defaultValue = "30") int before) {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(before);
        int deletedNodes = traceNodeMapper.deleteBefore(cutoff);
        int deletedRuns = traceRunMapper.deleteBefore(cutoff);

        log.info("链路数据清理完成: 删除 {} 条 runs, {} 条 nodes ({} 天前)", deletedRuns, deletedNodes, before);
        return Response.ok(Map.of("deletedRuns", deletedRuns, "deletedNodes", deletedNodes));
    }

    // ---- VO ----

    @Data
    public static class TraceRunVO {
        private String traceId;
        private Long userId;
        private String entryMethod;
        private String status;
        private Long latencyMs;
        private String errorMsg;
        private LocalDateTime createdAt;
    }

    @Data
    public static class TraceNodeVO {
        private String nodeName;
        private String nodeType;
        private String status;
        private Long latencyMs;
        private String extraData;
        private Integer depth;
        private String parentNodeId;
        private LocalDateTime createdAt;
    }

    @Data
    public static class TraceDetailVO {
        private TraceRunVO trace;
        private List<TraceNodeVO> nodes;
    }

    @Data
    public static class PageResult<T> {
        private List<T> records;
        private long total;
        private int page;
        private int size;
    }
}
