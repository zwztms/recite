package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * user_streak 表 Mapper.
 */
@Mapper
public interface UserStreakMapper extends BaseMapper<UserStreakDO> {
}
