package cn.bugstack.recite.infrastructure.adapter.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * trace_runs 表 Mapper.
 */
@Mapper
public interface TraceRunMapper extends BaseMapper<TraceRunDO> {

    @Select("SELECT " +
            "COALESCE(COUNT(*) FILTER(WHERE created_at > CURRENT_DATE), 0) AS \"todayTotal\", " +
            "COALESCE(ROUND(AVG(latency_ms) FILTER(WHERE created_at > CURRENT_DATE)), 0) AS \"avgLatency\", " +
            "COALESCE(COUNT(*) FILTER(WHERE status = 'ERROR' AND created_at > CURRENT_DATE), 0) AS \"todayErrors\" " +
            "FROM trace_runs")
    Map<String, Object> selectTodayStats();

    @Delete("DELETE FROM trace_runs WHERE created_at < #{cutoff}")
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff);
}
