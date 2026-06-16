package cn.bugstack.recite.domain.recite.model.valueobj;

/**
 * 会话 ID 值对象。
 */
public record SessionId(String value) {
    public SessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
    }
}
