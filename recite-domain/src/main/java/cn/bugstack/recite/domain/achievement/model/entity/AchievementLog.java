package cn.bugstack.recite.domain.achievement.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 徽章获得记录实体 — 映射 achievement_log 表.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementLog {

    private Long id;
    private Long userId;
    private String badgeKey;
    private LocalDateTime earnedAt;
}
