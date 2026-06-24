package cn.bugstack.recite.infrastructure.adapter.rag;

import cn.bugstack.recite.domain.knowledge.port.out.HybridSearchPort;
import cn.bugstack.recite.infrastructure.adapter.persistence.KnowledgeChunkMapper;
import cn.bugstack.recite.infrastructure.adapter.persistence.KnowledgeChunkDO;
import cn.bugstack.recite.infrastructure.adapter.persistence.PvVectorTypeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BM25 + 向量 RRF(Reciprocal Rank Fusion) 混合搜索适配器.
 * 两路并行检索 → RRF 融合排序 → 输出 top-K.
 * RRF 公式: score = Σ 1/(k + rank_i), k=60.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchAdapter implements HybridSearchPort {

    private final KnowledgeChunkMapper mapper;
    private static final int RRF_K = 60;
    private static final int PER_CHANNEL_TOP = 20;

    @Override
    public List<HybridSearchResult> search(String queryText, float[] queryEmbedding, int topK) {
        // 1. 向量路: pgvector COSINE
        String vecStr = PvVectorTypeHandler.vectorToPgString(queryEmbedding);
        List<KnowledgeChunkDO> vectorResults = mapper.searchSimilar(vecStr, PER_CHANNEL_TOP);

        // 2. 关键词路: PG full-text ts_rank (BM25)
        String tsQuery = buildTsQuery(queryText);
        List<KnowledgeChunkDO> bm25Results = tsQuery.isEmpty()
                ? List.of()
                : mapper.searchFullText(tsQuery, PER_CHANNEL_TOP);

        // 3. RRF 融合
        List<HybridSearchResult> fused = rrfFusion(vectorResults, bm25Results, topK);

        log.debug("HybridSearch: vector={}, bm25={}, fused={}",
                vectorResults.size(), bm25Results.size(), fused.size());
        return fused;
    }

    /** 用户查询 → tsquery 格式 */
    private String buildTsQuery(String text) {
        if (text == null || text.isBlank()) return "";
        String[] words = text.split("[\\s，。！？、；：\"'（）\\[\\]【】]+");
        return Arrays.stream(words)
                .filter(w -> w.length() >= 2)
                .map(w -> w.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", ""))
                .filter(w -> w.length() >= 2)
                .collect(Collectors.joining(" & "));
    }

    private List<HybridSearchResult> rrfFusion(
            List<KnowledgeChunkDO> vectorResults,
            List<KnowledgeChunkDO> bm25Results, int topK) {

        Map<String, FusionScore> scoreMap = new LinkedHashMap<>();

        // 向量路 RRF
        for (int i = 0; i < vectorResults.size(); i++) {
            KnowledgeChunkDO c = vectorResults.get(i);
            scoreMap.putIfAbsent(c.getChunkText(), new FusionScore(c));
            FusionScore fs = scoreMap.get(c.getChunkText());
            fs.vectorScore = c.getSimilarity() != null ? c.getSimilarity().floatValue() : 0;
            fs.rrfSum += 1.0 / (RRF_K + i + 1);
        }

        // 关键词路 RRF
        for (int i = 0; i < bm25Results.size(); i++) {
            KnowledgeChunkDO c = bm25Results.get(i);
            scoreMap.putIfAbsent(c.getChunkText(), new FusionScore(c));
            FusionScore fs = scoreMap.get(c.getChunkText());
            fs.bm25Score = c.getSimilarity() != null ? c.getSimilarity().floatValue() : 0;
            fs.rrfSum += 1.0 / (RRF_K + i + 1);
        }

        return scoreMap.values().stream()
                .sorted((a, b) -> Double.compare(b.rrfSum, a.rrfSum))
                .limit(topK)
                .map(fs -> new HybridSearchResult(
                        fs.chunk.getChunkText(),
                        (float) fs.rrfSum, fs.vectorScore, fs.bm25Score))
                .toList();
    }

    private static class FusionScore {
        final KnowledgeChunkDO chunk;
        float vectorScore = 0, bm25Score = 0;
        double rrfSum = 0;
        FusionScore(KnowledgeChunkDO c) { this.chunk = c; }
    }
}
