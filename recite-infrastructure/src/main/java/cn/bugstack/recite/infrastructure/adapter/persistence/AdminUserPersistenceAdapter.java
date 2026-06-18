package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.auth.model.entity.AdminUserEntity;
import cn.bugstack.recite.domain.auth.port.out.AdminUserPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 管理员持久化适配器 — MyBatis Plus 实现.
 */
@Repository
@RequiredArgsConstructor
public class AdminUserPersistenceAdapter implements AdminUserPort {

    private final AdminUserMapper mapper;

    @Override
    public AdminUserEntity findByUsername(String username) {
        AdminUserDO d = mapper.selectOne(
                new LambdaQueryWrapper<AdminUserDO>().eq(AdminUserDO::getUsername, username));
        return toEntity(d);
    }

    @Override
    public boolean existsById(long id) {
        return mapper.selectCount(
                new LambdaQueryWrapper<AdminUserDO>().eq(AdminUserDO::getId, id)) > 0;
    }

    // ---- 转换 ----

    private AdminUserEntity toEntity(AdminUserDO d) {
        if (d == null) return null;
        return AdminUserEntity.builder()
                .id(d.getId()).username(d.getUsername()).passwordHash(d.getPasswordHash())
                .nickname(d.getNickname()).status(d.getStatus())
                .createdAt(d.getCreatedAt()).build();
    }
}
