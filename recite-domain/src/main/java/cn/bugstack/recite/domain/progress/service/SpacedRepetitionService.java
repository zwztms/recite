package cn.bugstack.recite.domain.progress.service;

import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 间隔重复算法 — 简化 FSRS，纯函数.
 */
@Service
public class SpacedRepetitionService {

    private static final double INITIAL_EASE = 2.5;
    private static final double MIN_EASE = 1.3;
    private static final double MAX_EASE = 5.0;
    private static final int INITIAL_INTERVAL = 1;

    /**
     * AI 评分后计算掌握度和间隔.
     *
     * @param current 当前进度（首次为 null）
     * @param aiScore AI 评分 1-10
     * @param userId  用户 ID
     * @param questionId 题目 ID
     * @param moduleKey 模块
     * @return 更新后的实体（由调用方持久化）
     */
    public UserProgressEntity calculateAfterScore(UserProgressEntity current,
                                                   int aiScore,
                                                   Long userId, String questionId,
                                                   String moduleKey) {
        if (current == null) {
            // 首次背诵
            return UserProgressEntity.builder()
                    .userId(userId).questionId(questionId).moduleKey(moduleKey)
                    .masteryScore(aiScore * 10)
                    .reciteCount(1)
                    .averageScore(aiScore)
                    .lastRecitedAt(LocalDateTime.now())
                    .nextReviewAt(LocalDateTime.now().plusDays(1))
                    .reviewInterval(INITIAL_INTERVAL)
                    .easeFactor(INITIAL_EASE)
                    .build();
        }

        // 加权平滑掌握度
        int newMastery = (int) (current.getMasteryScore() * 0.7 + (aiScore * 10) * 0.3);
        newMastery = Math.max(0, Math.min(100, newMastery));

        // 更新平均分
        double newAvg = (current.getAverageScore() * current.getReciteCount() + aiScore)
                / (current.getReciteCount() + 1);

        // 根据分数调整间隔和难度因子
        double ease = current.getEaseFactor();
        int interval = current.getReviewInterval();

        if (aiScore >= 8) {
            interval = (int) Math.round(interval * ease);
            ease = Math.min(ease + 0.1, MAX_EASE);
        } else if (aiScore >= 5) {
            interval = Math.max(interval, 1);
            // ease 不变
        } else {
            interval = 1; // 重置为 1 天
            ease = Math.max(ease - 0.2, MIN_EASE);
        }

        interval = Math.max(1, Math.min(365, interval));

        return UserProgressEntity.builder()
                .id(current.getId())
                .userId(userId).questionId(questionId).moduleKey(moduleKey)
                .masteryScore(newMastery)
                .reciteCount(current.getReciteCount() + 1)
                .averageScore(newAvg)
                .lastRecitedAt(LocalDateTime.now())
                .nextReviewAt(LocalDateTime.now().plusDays(interval))
                .reviewInterval(interval)
                .easeFactor(ease)
                .build();
    }
}
