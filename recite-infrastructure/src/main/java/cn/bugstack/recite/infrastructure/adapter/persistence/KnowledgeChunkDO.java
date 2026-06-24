package cn.bugstack.recite.infrastructure.adapter.persistence;

import lombok.Data;

/**
 * knowledge_chunks 表数据对象.
 */
@Data
public class KnowledgeChunkDO {
    private Long id;
    private String docTitle;
    private String docSource;
    private String chunkText;
    private int chunkIndex;
    private String enrichedContext;
    private String embedding;    // pgvector 字符串形式
    private String docMetadata;  // JSONB 字符串
    private String tag;
    private java.time.LocalDateTime createdAt;
    private Double similarity;   // 检索时动态计算
}
