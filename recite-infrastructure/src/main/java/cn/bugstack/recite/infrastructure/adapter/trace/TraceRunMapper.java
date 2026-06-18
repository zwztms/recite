package cn.bugstack.recite.infrastructure.adapter.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * trace_runs 表 Mapper.
 */
@Mapper
public interface TraceRunMapper extends BaseMapper<TraceRunDO> {
}
