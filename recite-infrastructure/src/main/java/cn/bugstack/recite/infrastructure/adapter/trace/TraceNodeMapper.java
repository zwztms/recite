package cn.bugstack.recite.infrastructure.adapter.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * trace_nodes 表 Mapper.
 */
@Mapper
public interface TraceNodeMapper extends BaseMapper<TraceNodeDO> {
}
