package cn.bugstack.recite.shared.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 聚合根基类 — 聚合的入口，负责维护内部一致性并收集领域事件。
 * <p>
 * 子类在执行业务操作后调用 {@link #registerEvent}，框架在持久化后调用
 * {@link #pullEvents} 获取待发布事件。事件由基础设施层统一发布。
 * <p>
 * 参考：Spring Data {@code AbstractAggregateRoot}（适配 MyBatis 场景的独立实现）
 */
public abstract class AggregateRoot<ID extends java.io.Serializable> extends Entity<ID> {

    private final List<DomainEvent> events = new ArrayList<>();

    /**
     * 注册领域事件。子类在业务方法内调用。
     */
    protected void registerEvent(DomainEvent event) {
        events.add(Objects.requireNonNull(event));
    }

    /**
     * 取出并清空待发布的领域事件。框架/仓储在持久化后调用。
     */
    public List<DomainEvent> pullEvents() {
        if (events.isEmpty()) return Collections.emptyList();
        List<DomainEvent> result = new ArrayList<>(events);
        events.clear();
        return Collections.unmodifiableList(result);
    }
}
