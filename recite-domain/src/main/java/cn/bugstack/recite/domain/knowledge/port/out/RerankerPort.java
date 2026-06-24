package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 重排序 SPI — LLM Cross-Encoder 打分重排.
 */
public interface RerankerPort {

    List<String> rerank(String query, List<String> candidates, int topK);
}
