package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.achievement.model.entity.AchievementLog;
import cn.bugstack.recite.domain.achievement.port.out.AchievementPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 徽章记录持久化适配器 — MyBatis Plus 实现.
 */
@Repository
@RequiredArgsConstructor
public class AchievementPersistenceAdapter implements AchievementPort {

    private final AchievementLogMapper mapper;

    @Override
    public void save(Long userId, String badgeKey, LocalDateTime earnedAt) {
        AchievementLogDO d = new AchievementLogDO();
        d.setUserId(userId);
        d.setBadgeKey(badgeKey);
        d.setEarnedAt(earnedAt);
        mapper.insert(d);
    }

    @Override
    public List<String> findEarnedBadgeKeys(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<AchievementLogDO>()
                        .eq(AchievementLogDO::getUserId, userId))
                .stream().map(AchievementLogDO::getBadgeKey).toList();
    }

    @Override
    public int countByUserId(Long userId) {
        return Math.toIntExact(mapper.selectCount(new LambdaQueryWrapper<AchievementLogDO>()
                .eq(AchievementLogDO::getUserId, userId)));
    }
}
