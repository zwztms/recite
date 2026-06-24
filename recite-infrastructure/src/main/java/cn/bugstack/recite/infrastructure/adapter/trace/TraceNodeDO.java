package cn.bugstack.recite.infrastructure.adapter.trace;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 trace_nodes 表 — 每个环节一条明细记录.
 */
@Data
@TableName("trace_nodes")
public class TraceNodeDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String nodeName;
    private String nodeType;
    private String status;
    private Long latencyMs;
    /** 阶段输入输出摘要 JSON: {"input":"...", "output":"...", "detail":"..."} */
    private String extraData;
    /** 节点深度（0=根阶段, 1=子步骤） */
    private Integer depth;
    /** 父节点 nodeId（根节点为 null） */
    private String parentNodeId;
    private LocalDateTime createdAt;
}
