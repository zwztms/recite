package cn.bugstack.recite.infrastructure.adapter.cache;

import cn.bugstack.recite.domain.recite.port.out.ScoreSlotPort;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 评分信号量适配器 — Redisson RSemaphore，10 permits.
 */
@Slf4j
@Service
public class ScoreSlotAdapter implements ScoreSlotPort {

    private static final String SLOT_KEY = "recite:score:slots";
    private static final int MAX_PERMITS = 10;

    private final RSemaphore semaphore;

    public ScoreSlotAdapter(RedissonClient redisson) {
        this.semaphore = redisson.getSemaphore(SLOT_KEY);
        // 首次初始化：如果不存在则设置 10
        this.semaphore.trySetPermits(MAX_PERMITS);
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
