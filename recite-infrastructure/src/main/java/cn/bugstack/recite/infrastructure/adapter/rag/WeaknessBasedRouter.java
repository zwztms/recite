package cn.bugstack.recite.infrastructure.adapter.rag;

import cn.bugstack.recite.domain.knowledge.port.out.KnowledgeRouterPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 基于薄弱标签的知识路由.
 * 纯规则驱动: 从 missedPoints 提取技术关键词 → 归类到标签 → 按频次分配配额.
 * 不需要 LLM，对标 Ragent 的树形分类器（但 recite 场景更简单）.
 */
@Slf4j
@Service
public class WeaknessBasedRouter implements KnowledgeRouterPort {

    /** 预定义标签 → 关键词映射 */
    private static final Map<String, List<String>> TAG_KEYWORDS = Map.of(
            "Java并发", List.of("volatile", "synchronized", "lock", "CAS", "AQS",
                    "线程池", "ThreadLocal", "并发", "原子"),
            "JVM", List.of("GC", "垃圾回收", "类加载", "双亲委派", "内存模型",
                    "堆", "栈", "方法区", "元空间", "JIT"),
            "集合", List.of("HashMap", "ConcurrentHashMap", "ArrayList",
                    "LinkedList", "红黑树", "哈希"),
            "Spring", List.of("IOC", "AOP", "Bean", "事务", "依赖注入",
                    "自动配置", "循环依赖"),
            "MySQL", List.of("索引", "B+树", "事务", "MVCC", "锁",
                    "SQL", "慢查询", "分库分表"),
            "Redis", List.of("缓存", "穿透", "击穿", "雪崩", "分布式锁",
                    "持久化", "集群"),
            "网络", List.of("TCP", "HTTP", "HTTPS", "三次握手", "四次挥手",
                    "OSI", "DNS"),
            "操作系统", List.of("进程", "线程", "死锁", "虚拟内存", "文件系统",
                    "上下文切换")
    );

    private static final int MIN_QUOTA = 2;

    @Override
    public RouteResult route(List<String> missedPoints, String moduleKey, int totalQuota) {
        if (missedPoints == null || missedPoints.isEmpty()) {
            return new RouteResult(
                    List.of(new TagAllocation(moduleKey, 1.0, totalQuota)), totalQuota);
        }

        // 统计每个标签命中次数
        Map<String, Integer> hitCount = new LinkedHashMap<>();
        for (String point : missedPoints) {
            for (var entry : TAG_KEYWORDS.entrySet()) {
                for (String kw : entry.getValue()) {
                    if (point.contains(kw)) {
                        hitCount.merge(entry.getKey(), 1, Integer::sum);
                        break;
                    }
                }
            }
        }

        if (hitCount.isEmpty()) {
            return new RouteResult(
                    List.of(new TagAllocation(moduleKey, 1.0, totalQuota)), totalQuota);
        }

        // 按频次比例分配配额
        int totalHits = hitCount.values().stream().mapToInt(Integer::intValue).sum();
        List<TagAllocation> allocations = new ArrayList<>();
        int allocated = 0;

        List<Map.Entry<String, Integer>> sorted = hitCount.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).toList();

        for (int i = 0; i < sorted.size(); i++) {
            var entry = sorted.get(i);
            double weight = (double) entry.getValue() / totalHits;
            int quota;
            if (i == sorted.size() - 1) {
                quota = totalQuota - allocated;
            } else {
                quota = Math.max(MIN_QUOTA, (int) Math.round(totalQuota * weight));
            }
            quota = Math.min(quota, totalQuota - allocated);
            allocations.add(new TagAllocation(entry.getKey(), weight, quota));
            allocated += quota;
        }

        log.debug("知识路由: {} missed → {} allocations", missedPoints.size(), allocations.size());
        return new RouteResult(allocations, totalQuota);
    }
}
