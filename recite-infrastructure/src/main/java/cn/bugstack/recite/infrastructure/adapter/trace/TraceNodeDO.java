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
    private LocalDateTime createdAt;
}
