package cn.bugstack.recite.domain.progress.port.out;

import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;

import java.util.List;
import java.util.Optional;

/**
 * 掌握度持久化 SPI.
 */
public interface ProgressPort {

    Optional<UserProgressEntity> findByUserAndQuestion(Long userId, String questionId);

    List<UserProgressEntity> findByUserId(Long userId);

    /** 到期复习题目: WHERE next_review_at <= NOW() ORDER BY next_review_at ASC */
    List<UserProgressEntity> findDueQuestions(Long userId, int limit);

    void save(UserProgressEntity progress);

    void update(UserProgressEntity progress);

    /** 已掌握数量: masteryScore >= 80 */
    int countMastered(Long userId);
}
