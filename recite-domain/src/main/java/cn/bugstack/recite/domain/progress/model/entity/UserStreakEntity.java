package cn.bugstack.recite.domain.progress.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户连续天数实体 — 映射 user_streak 表.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreakEntity {

    private Long userId;
    private int currentStreak;
    private LocalDate lastActiveDate;
    private int longestStreak;
}
