package cn.bugstack.recite.shared.model;

/**
 * 值对象基类 — 无唯一标识，通过属性值判定相等。
 * <p>
 * Java 17+ 环境下子类使用 {@code record} 实现，天然不可变、自动 equals/hashCode。
 * 此类主要作为类型标记和未来扩展点。
 */
public abstract class ValueObject {
}
