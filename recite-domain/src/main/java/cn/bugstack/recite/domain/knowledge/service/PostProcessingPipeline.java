package cn.bugstack.recite.domain.knowledge.service;

import cn.bugstack.recite.domain.knowledge.port.out.SearchPostProcessor;
import cn.bugstack.recite.domain.knowledge.port.out.SearchPostProcessor.PostProcessContext;
import cn.bugstack.recite.domain.knowledge.port.out.RerankerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 后处理管线 — 责任链模式依次执行所有启用的处理器.
 * 对标 Ragent PostProcessorChain.
 * 每步失败降级（跳过该处理器，保留上一步结果）.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostProcessingPipeline {

    private final List<SearchPostProcessor> processors;
    private final RerankerPort reranker;

    /**
     * 执行完整后处理管线.
     * @return top-K chunk 文本列表
     */
    public List<String> process(List<String> candidates, String query,
                                 float[] queryEmbedding, int topK) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        PostProcessContext ctx = new PostProcessContext(queryEmbedding);
        List<String> current = candidates;

        // 按 order 排序执行所有 enabled 处理器
        List<SearchPostProcessor> sorted = processors.stream()
                .filter(SearchPostProcessor::isEnabled)
                .sorted(Comparator.comparingInt(SearchPostProcessor::getOrder))
                .toList();

        for (SearchPostProcessor processor : sorted) {
            try {
                List<String> processed = processor.process(current, query, ctx);
                if (processed != null && !processed.isEmpty()) {
                    log.debug("后处理 {}: {} → {}", processor.getName(),
                            current.size(), processed.size());
                    current = processed;
                }
            } catch (Exception e) {
                log.warn("后处理器 {} 失败(降级跳过): {}", processor.getName(), e.getMessage());
            }
        }

        // 最终截取 topK
        return current.stream().limit(topK).toList();
    }
}
