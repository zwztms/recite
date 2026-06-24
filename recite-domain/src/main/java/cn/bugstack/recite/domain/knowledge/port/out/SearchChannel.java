package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 检索通道统一接口.
 * 对标 Ragent SearchChannel: getName()/isEnabled()/search()/getPriority().
 * 新加通道只需实现此接口并注册为 Spring Bean 即可自动加入多通道引擎.
 */
public interface SearchChannel {

    /** 通道唯一名称，如 "HybridSearch" */
    String getName();

    /** 优先级（数字越小越优先，去重时保留高优先级结果） */
    int getPriority();

    /** 是否启用（可根据上下文条件判断） */
    boolean isEnabled(SearchContext context);

    /** 执行检索 */
    ChannelResult search(SearchContext context);

    record SearchContext(List<String> queries, float[] queryEmbedding,
                         KnowledgeRouterPort.RouteResult route, int topK) {}

    record ChannelResult(String channelName, List<String> chunkTexts, long costMs) {}
}
