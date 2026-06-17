package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.auth.model.entity.UserEntity;
import cn.bugstack.recite.domain.auth.port.out.UserPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 用户持久化适配器 — MyBatis Plus 实现.
 */
@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPort {

    private final UserMapper mapper;

    @Override
    public UserEntity findByPhone(String phone) {
        UserDO d = mapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getPhone, phone));
        return toEntity(d);
    }

    @Override
    public UserEntity findById(Long id) {
        UserDO d = mapper.selectById(id);
        return toEntity(d);
    }

    @Override
    public void save(UserEntity user) {
        UserDO d = toDO(user);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        mapper.insert(d);
        user.setId(d.getId());
    }

    // ---- 转换 ----

    private UserEntity toEntity(UserDO d) {
        if (d == null) return null;
        return UserEntity.builder()
                .id(d.getId()).phone(d.getPhone()).passwordHash(d.getPasswordHash())
                .nickname(d.getNickname()).avatar(d.getAvatar())
                .role(d.getRole() != null ? d.getRole() : "USER")
                .status(d.getStatus()).lastLoginAt(d.getLastLoginAt())
                .createdAt(d.getCreatedAt()).build();
    }

    private UserDO toDO(UserEntity e) {
        UserDO d = new UserDO();
        d.setId(e.getId()); d.setPhone(e.getPhone()); d.setPasswordHash(e.getPasswordHash());
        d.setNickname(e.getNickname()); d.setAvatar(e.getAvatar());
        d.setRole(e.getRole() != null ? e.getRole() : "USER");
        d.setStatus(e.getStatus() != null ? e.getStatus() : "ACTIVE");
        d.setLastLoginAt(e.getLastLoginAt()); d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(LocalDateTime.now());
        return d;
    }
}
