package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * admin_users 表 Mapper.
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUserDO> {
}
