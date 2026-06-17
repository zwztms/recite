package cn.bugstack.recite.domain.progress.service;

import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 连续天数服务.
 */
@Service
public class StreakService {

    private final StreakPort streakPort;

    public StreakService(StreakPort streakPort) {
        this.streakPort = streakPort;
    }

    /**
     * 每日签到 — 每次结束背诵时调用.
     */
    public UserStreakEntity checkIn(Long userId) {
        Optional<UserStreakEntity> opt = streakPort.findByUserId(userId);
        LocalDate today = LocalDate.now();

        if (opt.isEmpty()) {
            // 首次
            UserStreakEntity e = UserStreakEntity.builder()
                    .userId(userId).currentStreak(1).lastActiveDate(today).longestStreak(1).build();
            streakPort.save(e);
            return e;
        }

        UserStreakEntity e = opt.get();
        LocalDate last = e.getLastActiveDate();

        if (last == null || last.isBefore(today.minusDays(1))) {
            // 断签
            e.setCurrentStreak(1);
        } else if (last.equals(today.minusDays(1))) {
            // 连续
            e.setCurrentStreak(e.getCurrentStreak() + 1);
        }
        // last.equals(today) → 同天多次，不变

        e.setLastActiveDate(today);
        e.setLongestStreak(Math.max(e.getLongestStreak(), e.getCurrentStreak()));
        streakPort.update(e);
        return e;
    }
}
