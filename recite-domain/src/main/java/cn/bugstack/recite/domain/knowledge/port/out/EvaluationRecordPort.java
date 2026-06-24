package cn.bugstack.recite.domain.knowledge.port.out;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评估记录持久化 SPI.
 */
public interface EvaluationRecordPort {

    void save(String sessionId, RAGEvaluatorPort.RAGEvaluationResult result,
              List<String> retrievedChunks, String aiSuggestion);

    EvalRecord findBySessionId(String sessionId);

    List<EvalRecord> getRecentStats(int limit);

    record EvalRecord(String sessionId, double faithfulness, double contextRelevance,
                      double contextRecall, double answerRelevance, LocalDateTime createdAt) {}
}
