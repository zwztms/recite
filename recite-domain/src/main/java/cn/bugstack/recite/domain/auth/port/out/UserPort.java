package cn.bugstack.recite.domain.auth.port.out;

import cn.bugstack.recite.domain.auth.model.entity.UserEntity;

/**
 * 用户持久化 SPI.
 */
public interface UserPort {

    /** 手机号查用户（注册去重） */
    UserEntity findByPhone(String phone);

    /** 按 ID 查（拦截器注入） */
    UserEntity findById(Long id);

    /** 新增用户 */
    void save(UserEntity user);
}
