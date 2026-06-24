package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * RAG 评估 SPI — RAGAS 四指标自动评估.
 */
public interface RAGEvaluatorPort {

    RAGEvaluationResult evaluate(String sessionId, String question, String answer,
                                  String aiSuggestion, List<String> retrievedChunks);

    record RAGEvaluationResult(double faithfulness, double contextRelevance,
                               double contextRecall, double answerRelevance) {}
}
