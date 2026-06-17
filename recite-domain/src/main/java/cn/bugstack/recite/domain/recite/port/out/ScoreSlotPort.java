package cn.bugstack.recite.domain.recite.port.out;

/**
 * Redis 评分并发信号量 SPI.
 */
public interface ScoreSlotPort {

    /** 尝试获取评分槽位，超时返回 false */
    boolean tryAcquire(long timeoutMs);

    /** 释放槽位 */
    void release();

    /** 剩余槽位数 */
    int available();
}
