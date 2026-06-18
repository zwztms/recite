package cn.bugstack.recite.infrastructure.adapter.cache;

import cn.bugstack.recite.domain.achievement.port.out.NewBadgePort;
import cn.bugstack.recite.types.annotation.ReciteTraceNode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 新徽章适配器 — 前端轮询用.
 *
 * <p>Key: recite:new_badges:{userId}，TTL 5 分钟.</p>
 */
@Slf4j
@Service
public class NewBadgeRedisAdapter implements NewBadgePort {

    private static final String KEY_PREFIX = "recite:new_badges:";
    private static final int TTL_SECONDS = 300;

    private final RedissonClient redisson;

    public NewBadgeRedisAdapter(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @ReciteTraceNode(type = "CACHE", name = "标记新徽章")
    @Override
    public void addNewBadges(Long userId, List<String> badgeKeys) {
        if (badgeKeys == null || badgeKeys.isEmpty()) return;
        String key = KEY_PREFIX + userId;
        RSet<String> set = redisson.getSet(key);
        set.addAll(badgeKeys);
        set.expire(TTL_SECONDS, TimeUnit.SECONDS);
        log.info("新徽章已写入 Redis: userId={}, keys={}", userId, badgeKeys);
    }

    @Override
    public List<String> getNewBadges(Long userId) {
        RSet<String> set = redisson.getSet(KEY_PREFIX + userId);
        return List.copyOf(set.readAll());
    }

    @Override
    public void ackNewBadges(Long userId) {
        redisson.getSet(KEY_PREFIX + userId).delete();
        log.info("新徽章已确认: userId={}", userId);
    }
}
