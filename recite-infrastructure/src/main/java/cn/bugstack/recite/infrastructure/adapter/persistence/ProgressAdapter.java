package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.types.annotation.ReciteTraceNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 掌握度持久化适配器 — MyBatis Plus 实现.
 */
@Repository
@RequiredArgsConstructor
public class ProgressAdapter implements ProgressPort {

    private final UserProgressMapper mapper;

    @Override
    public Optional<UserProgressEntity> findByUserAndQuestion(Long userId, String questionId) {
        UserProgressDO d = mapper.selectOne(new LambdaQueryWrapper<UserProgressDO>()
                .eq(UserProgressDO::getUserId, userId)
                .eq(UserProgressDO::getQuestionId, questionId));
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public List<UserProgressEntity> findByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<UserProgressDO>()
                        .eq(UserProgressDO::getUserId, userId))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<UserProgressEntity> findDueQuestions(Long userId, int limit) {
        return mapper.findDueQuestions(userId, limit).stream()
                .map(this::toEntity).toList();
    }

    @ReciteTraceNode(type = "DB", name = "保存掌握度")
    @Override
    public void save(UserProgressEntity progress) {
        mapper.insert(toDO(progress));
    }

    @ReciteTraceNode(type = "DB", name = "更新掌握度")
    @Override
    public void update(UserProgressEntity progress) {
        UserProgressDO d = toDO(progress);
        d.setId(progress.getId());
        mapper.updateById(d);
    }

    @Override
    public int countMastered(Long userId) {
        return mapper.countMastered(userId);
    }

    // ---- 转换 ----

    private UserProgressEntity toEntity(UserProgressDO d) {
        return UserProgressEntity.builder()
                .id(d.getId()).userId(d.getUserId()).questionId(d.getQuestionId())
                .moduleKey(d.getModuleKey()).masteryScore(d.getMasteryScore() != null ? d.getMasteryScore() : 0)
                .reciteCount(d.getReciteCount() != null ? d.getReciteCount() : 0)
                .averageScore(d.getAverageScore() != null ? d.getAverageScore() : 0)
                .lastRecitedAt(d.getLastRecitedAt()).nextReviewAt(d.getNextReviewAt())
                .reviewInterval(d.getReviewInterval() != null ? d.getReviewInterval() : 1)
                .easeFactor(d.getEaseFactor() != null ? d.getEaseFactor() : 2.5).build();
    }

    private UserProgressDO toDO(UserProgressEntity e) {
        UserProgressDO d = new UserProgressDO();
        d.setId(e.getId()); d.setUserId(e.getUserId()); d.setQuestionId(e.getQuestionId());
        d.setModuleKey(e.getModuleKey()); d.setMasteryScore(e.getMasteryScore());
        d.setReciteCount(e.getReciteCount()); d.setAverageScore(e.getAverageScore());
        d.setLastRecitedAt(e.getLastRecitedAt()); d.setNextReviewAt(e.getNextReviewAt());
        d.setReviewInterval(e.getReviewInterval()); d.setEaseFactor(e.getEaseFactor());
        return d;
    }
}
