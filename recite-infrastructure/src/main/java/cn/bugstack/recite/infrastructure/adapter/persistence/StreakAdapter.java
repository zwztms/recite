package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 连续天数持久化适配器 — MyBatis Plus 实现.
 */
@Repository
@RequiredArgsConstructor
public class StreakAdapter implements StreakPort {

    private final UserStreakMapper mapper;

    @Override
    public Optional<UserStreakEntity> findByUserId(Long userId) {
        UserStreakDO d = mapper.selectById(userId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public void save(UserStreakEntity streak) {
        mapper.insert(toDO(streak));
    }

    @Override
    public void update(UserStreakEntity streak) {
        mapper.updateById(toDO(streak));
    }

    // ---- 转换 ----

    private UserStreakEntity toEntity(UserStreakDO d) {
        return UserStreakEntity.builder()
                .userId(d.getUserId()).currentStreak(d.getCurrentStreak() != null ? d.getCurrentStreak() : 0)
                .lastActiveDate(d.getLastActiveDate())
                .longestStreak(d.getLongestStreak() != null ? d.getLongestStreak() : 0).build();
    }

    private UserStreakDO toDO(UserStreakEntity e) {
        UserStreakDO d = new UserStreakDO();
        d.setUserId(e.getUserId()); d.setCurrentStreak(e.getCurrentStreak());
        d.setLastActiveDate(e.getLastActiveDate()); d.setLongestStreak(e.getLongestStreak());
        return d;
    }
}
