package cn.bugstack.recite.domain.knowledge.service;

import cn.bugstack.recite.domain.knowledge.port.out.SearchPostProcessor;
import cn.bugstack.recite.domain.knowledge.port.out.EmbeddingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MMR (Maximal Marginal Relevance) 多样性处理器.
 * 公式: MMR = argmax[ λ·sim(chunk, query) - (1-λ)·max_sim(chunk, already_selected) ]
 * order=3, 在去重之后、Rerank 之前执行.
 */
@Service
@RequiredArgsConstructor
public class MmrDiversifier implements SearchPostProcessor {

    private final EmbeddingPort embeddingPort;
    private static final double LAMBDA = 0.7;

    @Override public String getName() { return "MMRDiversifier"; }
    @Override public int getOrder() { return 3; }

    @Override
    public List<String> process(List<String> candidates, String query, PostProcessContext ctx) {
        if (candidates == null || candidates.size() <= 5) return candidates;

        float[] queryEmb = ctx.queryEmbedding() != null
                ? ctx.queryEmbedding() : embeddingPort.embed(query);

        List<String> selected = new ArrayList<>();
        List<String> remaining = new ArrayList<>(candidates);

        // 缓存 embedding
        Map<String, float[]> embCache = new HashMap<>();
        for (String c : candidates) {
            try { embCache.put(c, embeddingPort.embed(c)); } catch (Exception e) {
                // 静默跳过单条 embedding 失败
            }
        }

        // 贪心选择
        while (!remaining.isEmpty() && selected.size() < candidates.size()) {
            String best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (String c : remaining) {
                float[] cEmb = embCache.get(c);
                if (cEmb == null) continue;

                double simToQuery = cosine(cEmb, queryEmb);
                double maxSimToSelected = selected.stream()
                        .mapToDouble(s -> cosine(embCache.get(s), cEmb))
                        .max().orElse(0);

                double mmr = LAMBDA * simToQuery - (1 - LAMBDA) * maxSimToSelected;
                if (mmr > bestScore) { bestScore = mmr; best = c; }
            }

            if (best != null) {
                selected.add(best);
                remaining.remove(best);
            } else break;
        }

        return selected;
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
