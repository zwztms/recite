package cn.bugstack.recite.domain.auth.port.out;

import cn.bugstack.recite.domain.auth.model.entity.AdminUserEntity;

/**
 * 管理员持久化 SPI.
 */
public interface AdminUserPort {

    /** 按用户名查询 */
    AdminUserEntity findByUsername(String username);
}
