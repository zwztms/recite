package cn.bugstack.recite.domain.recite.model.event;

import cn.bugstack.recite.shared.model.DomainEvent;

/**
 * 会话完成领域事件 — 通知成就/进度/报告子域。
 */
public record SessionFinishedEvent(String sessionId, long occurredAt)
        implements DomainEvent {
}
