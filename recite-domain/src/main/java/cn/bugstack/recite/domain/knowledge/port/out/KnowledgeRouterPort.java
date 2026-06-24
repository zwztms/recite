package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 知识路由 SPI — 根据薄弱点分布分配检索配额.
 * 对标 Ragent resolveIntents.
 */
public interface KnowledgeRouterPort {

    RouteResult route(List<String> missedPoints, String moduleKey, int totalQuota);

    record RouteResult(List<TagAllocation> allocations, int totalQuota) {}

    record TagAllocation(String tag, double weight, int quota) {}
}
