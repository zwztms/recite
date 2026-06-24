package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 知识 Chunk 持久化 SPI.
 */
public interface KnowledgeChunkPort {

    void insert(String docTitle, String docSource,
                ChunkEnricherPort.EnrichedChunk chunk, float[] embedding);

    List<String> searchSimilar(float[] embedding, int topK);

    List<String> searchFullText(String query, int topK);

    List<String> searchByTag(float[] embedding, String tag, int topK);

    List<DocSummary> listDocuments();

    void deleteByDocTitle(String docTitle);

    record DocSummary(String docTitle, String docSource, int chunkCount) {}
}
