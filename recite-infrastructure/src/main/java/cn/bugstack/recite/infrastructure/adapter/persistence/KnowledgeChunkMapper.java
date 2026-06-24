package cn.bugstack.recite.infrastructure.adapter.persistence;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * knowledge_chunks 表 MyBatis Mapper.
 * pgvector 查询使用原生向量操作符 <=> (cosine distance).
 */
@Mapper
public interface KnowledgeChunkMapper {

    @Insert("""
            INSERT INTO knowledge_chunks (doc_title, doc_source, chunk_text, chunk_index,
            enriched_context, embedding, doc_metadata, tag)
            VALUES (#{docTitle}, #{docSource}, #{chunkText}, #{chunkIndex},
            #{enrichedContext}, #{embedding}::vector, #{docMetadata}::jsonb, #{tag})
            """)
    void insert(KnowledgeChunkDO chunk);

    /** 向量余弦相似度搜索 */
    @Select("""
            SELECT *, embedding <=> #{queryEmbedding}::vector AS similarity
            FROM knowledge_chunks
            ORDER BY embedding <=> #{queryEmbedding}::vector
            LIMIT #{topK}
            """)
    List<KnowledgeChunkDO> searchSimilar(@Param("queryEmbedding") String vectorStr,
                                         @Param("topK") int topK);

    /** BM25 关键词搜索 —— PG full-text ts_rank */
    @Select("""
            SELECT *, ts_rank(tsv, to_tsquery('simple', #{query})) AS similarity
            FROM knowledge_chunks
            WHERE tsv @@ to_tsquery('simple', #{query})
            ORDER BY similarity DESC
            LIMIT #{topK}
            """)
    List<KnowledgeChunkDO> searchFullText(@Param("query") String tsQuery,
                                          @Param("topK") int topK);

    /** 按标签过滤的向量搜索 */
    @Select("""
            SELECT *, embedding <=> #{queryEmbedding}::vector AS similarity
            FROM knowledge_chunks
            WHERE tag = #{tag}
            ORDER BY embedding <=> #{queryEmbedding}::vector
            LIMIT #{topK}
            """)
    List<KnowledgeChunkDO> searchByTag(@Param("queryEmbedding") String vectorStr,
                                       @Param("tag") String tag,
                                       @Param("topK") int topK);

    @Select("SELECT * FROM knowledge_chunks WHERE doc_title = #{docTitle} ORDER BY chunk_index")
    List<KnowledgeChunkDO> findByDocTitle(@Param("docTitle") String docTitle);

    @Select("SELECT DISTINCT doc_title, doc_source, count(*) as cnt, max(created_at) as created_at FROM knowledge_chunks GROUP BY doc_title, doc_source ORDER BY created_at DESC")
    List<DocSummary> listDocuments();

    @Delete("DELETE FROM knowledge_chunks WHERE doc_title = #{docTitle}")
    int deleteByDocTitle(@Param("docTitle") String docTitle);

    /** Mapper 内部 record，用于文档列表聚合查询 */
    record DocSummary(String docTitle, String docSource, int cnt, java.time.LocalDateTime createdAt) {}
}
