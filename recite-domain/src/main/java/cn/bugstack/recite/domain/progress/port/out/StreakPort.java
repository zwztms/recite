package cn.bugstack.recite.domain.progress.port.out;

import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;

import java.util.Optional;

/**
 * 连续天数持久化 SPI.
 */
public interface StreakPort {

    Optional<UserStreakEntity> findByUserId(Long userId);

    void save(UserStreakEntity streak);

    void update(UserStreakEntity streak);
}
