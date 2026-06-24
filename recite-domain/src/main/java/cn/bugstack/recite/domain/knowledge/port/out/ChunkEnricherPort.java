package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * Chunk 富化 SPI — LLM 为每个 chunk 生成上下文背景描述.
 */
public interface ChunkEnricherPort {

    List<EnrichedChunk> enrich(List<ChunkerPort.Chunk> chunks);

    record EnrichedChunk(String chunkText, int chunkIndex,
                         String contextSummary, String docTitle) {
        /** 用于向量化的完整文本：上下文摘要 + 正文 */
        public String getFullText() {
            return (contextSummary != null && !contextSummary.isEmpty()
                    ? contextSummary + "\n" : "") + chunkText;
        }
    }
}
