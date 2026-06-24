package cn.bugstack.recite.infrastructure.adapter.persistence;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RagEvaluationMapper {

    @Insert("""
            INSERT INTO rag_evaluations (session_id, faithfulness, context_relevance,
            context_recall, answer_relevance, retrieved_chunks, ai_suggestion_text)
            VALUES (#{sessionId}, #{faithfulness}, #{contextRelevance},
            #{contextRecall}, #{answerRelevance},
            #{retrievedChunks}::jsonb, #{aiSuggestionText})
            """)
    void insert(RagEvaluationDO eval);

    @Select("SELECT * FROM rag_evaluations WHERE session_id = #{sessionId}")
    RagEvaluationDO findBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM rag_evaluations ORDER BY created_at DESC LIMIT #{limit}")
    List<RagEvaluationDO> findRecent(@Param("limit") int limit);
}
