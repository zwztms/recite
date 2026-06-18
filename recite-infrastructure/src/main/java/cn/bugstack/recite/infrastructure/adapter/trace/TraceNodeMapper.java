package cn.bugstack.recite.infrastructure.adapter.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * trace_nodes 表 Mapper.
 */
@Mapper
public interface TraceNodeMapper extends BaseMapper<TraceNodeDO> {

    @Select("SELECT * FROM trace_nodes WHERE trace_id = #{traceId} ORDER BY id ASC")
    List<TraceNodeDO> selectByTraceId(@Param("traceId") String traceId);

    @Delete("DELETE FROM trace_nodes WHERE created_at < #{cutoff}")
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff);
}
