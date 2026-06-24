package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.knowledge.port.out.KnowledgeChunkPort;
import cn.bugstack.recite.domain.knowledge.port.out.ChunkEnricherPort.EnrichedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KnowledgeChunkPort 适配器 —— 桥接 domain SPI ↔ MyBatis Mapper.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkAdapter implements KnowledgeChunkPort {

    private final KnowledgeChunkMapper mapper;

    @Override
    public void insert(String docTitle, String docSource, EnrichedChunk chunk, float[] embedding) {
        KnowledgeChunkDO d = new KnowledgeChunkDO();
        d.setDocTitle(docTitle);
        d.setDocSource(docSource);
        d.setChunkText(chunk.chunkText());
        d.setChunkIndex(chunk.chunkIndex());
        d.setEnrichedContext(chunk.contextSummary());
        d.setEmbedding(PvVectorTypeHandler.vectorToPgString(embedding));
        d.setDocMetadata("{}");
        d.setTag("knowledge");
        mapper.insert(d);
    }

    @Override
    public List<String> searchSimilar(float[] embedding, int topK) {
        String v = PvVectorTypeHandler.vectorToPgString(embedding);
        return mapper.searchSimilar(v, topK).stream()
                .map(KnowledgeChunkDO::getChunkText).toList();
    }

    @Override
    public List<String> searchFullText(String query, int topK) {
        return mapper.searchFullText(query, topK).stream()
                .map(KnowledgeChunkDO::getChunkText).toList();
    }

    @Override
    public List<String> searchByTag(float[] embedding, String tag, int topK) {
        String v = PvVectorTypeHandler.vectorToPgString(embedding);
        return mapper.searchByTag(v, tag, topK).stream()
                .map(KnowledgeChunkDO::getChunkText).toList();
    }

    @Override
    public List<DocSummary> listDocuments() {
        return mapper.listDocuments().stream()
                .map(d -> new DocSummary(d.docTitle(), d.docSource(), d.cnt()))
                .toList();
    }

    @Override
    public void deleteByDocTitle(String docTitle) {
        mapper.deleteByDocTitle(docTitle);
    }
}
