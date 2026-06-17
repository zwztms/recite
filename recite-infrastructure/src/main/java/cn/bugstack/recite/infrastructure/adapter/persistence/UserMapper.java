package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * users 表 Mapper.
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
