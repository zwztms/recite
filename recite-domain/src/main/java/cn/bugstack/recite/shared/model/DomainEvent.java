package cn.bugstack.recite.shared.model;

/**
 * 领域事件标记接口。
 * 所有跨子域通信的事件必须实现此接口。
 */
public interface DomainEvent {
    long occurredAt();
}
