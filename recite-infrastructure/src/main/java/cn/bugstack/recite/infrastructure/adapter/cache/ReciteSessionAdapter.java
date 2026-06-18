package cn.bugstack.recite.infrastructure.adapter.cache;

import cn.bugstack.recite.domain.recite.model.entity.ReciteSession;
import cn.bugstack.recite.domain.recite.port.out.ReciteSessionPort;
import cn.bugstack.recite.types.annotation.ReciteTraceNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 会话存储适配器 — Redisson RBucket + Jackson 序列化，2h TTL.
 */
@Slf4j
@Service
public class ReciteSessionAdapter implements ReciteSessionPort {

    private static final String KEY_PREFIX = "recite:session:";
    private static final Duration TTL = Duration.ofHours(2);

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    public ReciteSessionAdapter(RedissonClient redisson) {
        this.redisson = redisson;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void save(ReciteSession session) {
        String json = toJson(session);
        RBucket<String> bucket = redisson.getBucket(key(session.getSessionId()));
        bucket.set(json, TTL);
    }

    @ReciteTraceNode(type = "CACHE", name = "读取背诵会话")
    @Override
    public Optional<ReciteSession> findById(String sessionId) {
        RBucket<String> bucket = redisson.getBucket(key(sessionId));
        String json = bucket.get();
        if (json == null) return Optional.empty();
        return Optional.ofNullable(fromJson(json));
    }

    @ReciteTraceNode(type = "CACHE", name = "更新会话缓存")
    @Override
    public void update(ReciteSession session) {
        // 先取当前 TTL，写入后恢复
        RBucket<String> bucket = redisson.getBucket(key(session.getSessionId()));
        long remain = bucket.remainTimeToLive();
        String json = toJson(session);
        bucket.set(json, remain > 0 ? Duration.ofMillis(remain) : TTL);
    }

    @Override
    public void delete(String sessionId) {
        redisson.getBucket(key(sessionId)).delete();
    }

    // ---- 序列化 ----

    private String toJson(ReciteSession s) {
        try {
            return objectMapper.writeValueAsString(s);
        } catch (JsonProcessingException e) {
            log.error("序列化 ReciteSession 失败", e);
            throw new RuntimeException(e);
        }
    }

    private ReciteSession fromJson(String json) {
        try {
            return objectMapper.readValue(json, ReciteSession.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化 ReciteSession 失败", e);
            return null;
        }
    }

    private static String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
