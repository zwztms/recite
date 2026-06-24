package cn.bugstack.recite.domain.recite.port.out;

/**
 * Skill 执行并发信号量 SPI — 与 ScoreSlotPort 模式一致，独立管理.
 * 评分槽释放后 skill 在独立槽中执行，不阻塞后续评分.
 */
public interface SkillSlotPort {
    boolean tryAcquire(long timeoutMs);
    void release();
    int available();
}
