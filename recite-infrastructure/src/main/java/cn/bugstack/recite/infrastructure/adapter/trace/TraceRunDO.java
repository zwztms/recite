package cn.bugstack.recite.infrastructure.adapter.trace;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 trace_runs 表 — 每次请求一个根记录.
 */
@Data
@TableName("trace_runs")
public class TraceRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long userId;
    private String entryMethod;
    private String status;
    private Long latencyMs;
    private String errorMsg;
    private LocalDateTime createdAt;
}
