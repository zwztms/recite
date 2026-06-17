package cn.bugstack.recite.domain.progress.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户掌握度实体 — 映射 user_progress 表.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgressEntity {

    private Long id;
    private Long userId;
    private String questionId;
    private String moduleKey;
    /** 0-100，替代旧 MasteryLevel 枚举 */
    private int masteryScore;
    private int reciteCount;
    private double averageScore;
    private LocalDateTime lastRecitedAt;
    /** 下次复习时间 */
    private LocalDateTime nextReviewAt;
    /** 当前间隔(天)，范围 1-365 */
    private int reviewInterval;
    /** 难度因子，范围 1.3-5.0，默认 2.5 */
    private double easeFactor;
}
