package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.knowledge.port.out.EvaluationRecordPort;
import cn.bugstack.recite.domain.knowledge.port.out.RAGEvaluatorPort.RAGEvaluationResult;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagEvaluationAdapter implements EvaluationRecordPort {

    private final RagEvaluationMapper mapper;
    private final Gson gson = new Gson();

    @Override
    public void save(String sessionId, RAGEvaluationResult result,
                     List<String> retrievedChunks, String aiSuggestion) {
        RagEvaluationDO d = new RagEvaluationDO();
        d.setSessionId(sessionId);
        d.setFaithfulness(result.faithfulness());
        d.setContextRelevance(result.contextRelevance());
        d.setContextRecall(result.contextRecall());
        d.setAnswerRelevance(result.answerRelevance());
        d.setRetrievedChunks(gson.toJson(retrievedChunks != null ? retrievedChunks : List.of()));
        d.setAiSuggestionText(aiSuggestion != null && aiSuggestion.length() > 500
                ? aiSuggestion.substring(0, 500) : aiSuggestion);
        mapper.insert(d);
    }

    @Override
    public EvalRecord findBySessionId(String sessionId) {
        RagEvaluationDO d = mapper.findBySessionId(sessionId);
        if (d == null) return null;
        return toRecord(d);
    }

    @Override
    public List<EvalRecord> getRecentStats(int limit) {
        return mapper.findRecent(limit).stream()
                .map(this::toRecord).toList();
    }

    private EvalRecord toRecord(RagEvaluationDO d) {
        return new EvalRecord(d.getSessionId(),
                d.getFaithfulness() != null ? d.getFaithfulness() : 0,
                d.getContextRelevance() != null ? d.getContextRelevance() : 0,
                d.getContextRecall() != null ? d.getContextRecall() : 0,
                d.getAnswerRelevance() != null ? d.getAnswerRelevance() : 0,
                d.getCreatedAt());
    }
}
