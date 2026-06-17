package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * recite_records 持久化适配器 — MyBatis Plus 实现.
 */
@Repository
@RequiredArgsConstructor
public class ReciteRecordAdapter implements ReciteRecordPort {

    private final ReciteRecordMapper mapper;

    @Override
    public ReciteRecordEntity save(ReciteRecordEntity record) {
        ReciteRecordDO d = toDO(record);
        d.setCreatedAt(LocalDateTime.now());
        mapper.insert(d);
        record.setId(d.getId());
        return record;
    }

    @Override
    public void updateFollowUp(Long recordId, String answer, String feedback) {
        ReciteRecordDO d = mapper.selectById(recordId);
        if (d != null) {
            d.setFollowUpAnswer(answer);
            d.setFollowUpFeedback(feedback);
            mapper.updateById(d);
        }
    }

    @Override
    public List<ReciteRecordEntity> findBySessionId(Long userId, String sessionId) {
        return mapper.selectList(new LambdaQueryWrapper<ReciteRecordDO>()
                        .eq(ReciteRecordDO::getUserId, userId)
                        .eq(ReciteRecordDO::getSessionId, sessionId)
                        .orderByAsc(ReciteRecordDO::getCreatedAt))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ReciteRecordEntity> findByUserId(Long userId, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<ReciteRecordDO>()
                        .eq(ReciteRecordDO::getUserId, userId)
                        .orderByDesc(ReciteRecordDO::getCreatedAt)
                        .last("LIMIT " + limit))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public ReciteRecordEntity findById(Long recordId) {
        ReciteRecordDO d = mapper.selectById(recordId);
        return d != null ? toEntity(d) : null;
    }

    @Override
    public int countByUserId(Long userId) {
        return mapper.countByUserId(userId);
    }

    @Override
    public double avgScoreByUserId(Long userId) {
        return mapper.avgScoreByUserId(userId);
    }

    @Override
    public int countByModule(Long userId, String moduleKey) {
        return mapper.countByModule(userId, moduleKey);
    }

    @Override
    public int countPerfectScores(Long userId) {
        return mapper.countPerfectScores(userId);
    }

    @Override
    public int countSessionsByUserId(Long userId) {
        return mapper.countSessionsByUserId(userId);
    }

    // ---- 转换 ----

    private ReciteRecordEntity toEntity(ReciteRecordDO d) {
        return ReciteRecordEntity.builder()
                .id(d.getId()).userId(d.getUserId()).sessionId(d.getSessionId())
                .mode(d.getMode()).moduleKey(d.getModuleKey()).questionId(d.getQuestionId())
                .userAnswer(d.getUserAnswer()).score(d.getScore()).feedback(d.getFeedback())
                .followUpQuestion(d.getFollowUpQuestion()).followUpAnswer(d.getFollowUpAnswer())
                .followUpFeedback(d.getFollowUpFeedback()).followUpDepth(
                        d.getFollowUpDepth() != null ? d.getFollowUpDepth() : 0)
                .parentRecordId(d.getParentRecordId())
                .responseTimeSeconds(d.getResponseTimeSeconds())
                .createdAt(d.getCreatedAt()).build();
    }

    private ReciteRecordDO toDO(ReciteRecordEntity e) {
        ReciteRecordDO d = new ReciteRecordDO();
        d.setId(e.getId()); d.setUserId(e.getUserId()); d.setSessionId(e.getSessionId());
        d.setMode(e.getMode()); d.setModuleKey(e.getModuleKey()); d.setQuestionId(e.getQuestionId());
        d.setUserAnswer(e.getUserAnswer()); d.setScore(e.getScore()); d.setFeedback(e.getFeedback());
        d.setFollowUpQuestion(e.getFollowUpQuestion());
        d.setFollowUpAnswer(e.getFollowUpAnswer());
        d.setFollowUpFeedback(e.getFollowUpFeedback());
        d.setFollowUpDepth(e.getFollowUpDepth());
        d.setParentRecordId(e.getParentRecordId());
        d.setResponseTimeSeconds(e.getResponseTimeSeconds());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }
}
