package cn.bugstack.recite.domain.knowledge.service;

import cn.bugstack.recite.domain.knowledge.port.out.*;
import cn.bugstack.recite.domain.knowledge.port.out.SearchChannel.ChannelResult;
import cn.bugstack.recite.domain.knowledge.port.out.SearchChannel.SearchContext;
import cn.bugstack.recite.domain.knowledge.port.out.KnowledgeRouterPort.RouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 多通道检索引擎.
 * 并行调度所有 enabled 的 SearchChannel，合并去重后输出 candidates.
 * 对标 Ragent MultiChannelRetrievalEngine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiChannelRetrievalEngine {

    private final List<SearchChannel> channels;

    /**
     * 执行多通道检索.
     * @param queries    改写后的查询列表（主query + 子queries）
     * @param route      意图路由结果（标签→配额）
     * @param primaryEmb 主 query 的 embedding
     * @param topK       每个通道检索数量
     * @return 去重合并后的 chunk 文本列表
     */
    public List<String> retrieve(List<String> queries, RouteResult route,
                                  float[] primaryEmb, int topK) {
        SearchContext ctx = new SearchContext(queries, primaryEmb, route, topK);
        Set<String> seen = new LinkedHashSet<>();
        List<String> results = new ArrayList<>();

        // 并行执行所有启用的通道
        List<ChannelResult> channelResults = channels.stream()
                .filter(ch -> {
                    boolean enabled = ch.isEnabled(ctx);
                    if (!enabled) log.debug("通道 {} 未启用，跳过", ch.getName());
                    return enabled;
                })
                .map(ch -> {
                    long start = System.currentTimeMillis();
                    ChannelResult cr = ch.search(ctx);
                    long cost = System.currentTimeMillis() - start;
                    log.debug("通道 {} 完成: {} 条, {}ms", ch.getName(),
                            cr.chunkTexts().size(), cost);
                    return cr;
                })
                .sorted((a, b) -> Integer.compare(
                        getChannelPriority(a.channelName()),
                        getChannelPriority(b.channelName())))
                .toList();

        // 合并去重（高优先级通道的结果排前面）
        for (ChannelResult cr : channelResults) {
            for (String chunk : cr.chunkTexts()) {
                if (seen.add(chunk)) results.add(chunk);
            }
        }

        log.debug("多通道检索: {} 通道 → {} 候选 (去重后)",
                channelResults.size(), results.size());
        return results;
    }

    private int getChannelPriority(String name) {
        return channels.stream()
                .filter(ch -> ch.getName().equals(name))
                .map(SearchChannel::getPriority).findFirst().orElse(99);
    }
}
