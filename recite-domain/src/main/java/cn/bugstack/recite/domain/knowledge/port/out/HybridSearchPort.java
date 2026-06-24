package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 混合搜索 SPI — BM25(关键词) + 向量(语义) 双路 RRF 融合.
 */
public interface HybridSearchPort {

    List<HybridSearchResult> search(String queryText, float[] queryEmbedding, int topK);

    record HybridSearchResult(String chunkText, float hybridScore,
                              float vectorScore, float bm25Score) {}
}
