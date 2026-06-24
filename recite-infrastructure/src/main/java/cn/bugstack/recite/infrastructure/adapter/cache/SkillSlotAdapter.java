package cn.bugstack.recite.infrastructure.adapter.cache;

import cn.bugstack.recite.domain.recite.port.out.SkillSlotPort;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis Skill 执行信号量 — Redisson RSemaphore，5 permits.
 * 与 ScoreSlotAdapter 模式完全一致，独立 key.
 */
@Slf4j
@Service
public class SkillSlotAdapter implements SkillSlotPort {

    private static final String KEY = "recite:skill:slots";
    private final RSemaphore semaphore;

    public SkillSlotAdapter(RedissonClient redisson) {
        this.semaphore = redisson.getSemaphore(KEY);
        this.semaphore.trySetPermits(5);
    }

    @Override
    public boolean tryAcquire(long timeoutMs) {
        try {
            return semaphore.tryAcquire(1, timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void release() {
        semaphore.release();
    }

    @Override
    public int available() {
        return semaphore.availablePermits();
    }
}
