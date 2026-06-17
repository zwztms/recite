package cn.bugstack.recite.domain.recite.port.out;

import cn.bugstack.recite.domain.recite.model.entity.ReciteSession;

import java.util.Optional;

/**
 * Redis 会话读写 SPI.
 */
public interface ReciteSessionPort {

    /** 写入 Redis，TTL 2h */
    void save(ReciteSession session);

    /** 读取 */
    Optional<ReciteSession> findById(String sessionId);

    /** 更新（保持原有 TTL） */
    void update(ReciteSession session);

    /** 删除 */
    void delete(String sessionId);
}
