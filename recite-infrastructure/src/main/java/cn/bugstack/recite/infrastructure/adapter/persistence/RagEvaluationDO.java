package cn.bugstack.recite.infrastructure.adapter.persistence;

import lombok.Data;

/**
 * rag_evaluations 表数据对象.
 */
@Data
public class RagEvaluationDO {
    private Long id;
    private String sessionId;
    private Double faithfulness;
    private Double contextRelevance;
    private Double contextRecall;
    private Double answerRelevance;
    private String retrievedChunks;   // JSONB
    private String aiSuggestionText;
    private java.time.LocalDateTime createdAt;
}
