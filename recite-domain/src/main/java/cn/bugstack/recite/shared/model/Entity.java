package cn.bugstack.recite.shared.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * 实体基类 — 有唯一标识的领域对象。
 * 所有子域实体必须继承此类或 AggregateRoot。
 *
 * 参考：Eric Evans《领域驱动设计》Shared Kernel 模式
 */
public abstract class Entity<ID extends Serializable> {

    public abstract ID getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity<?> other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getId() + "]";
    }
}
