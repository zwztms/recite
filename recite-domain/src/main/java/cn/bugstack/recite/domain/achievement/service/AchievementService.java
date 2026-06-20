package cn.bugstack.recite.domain.achievement.service;

import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeDefinition;
import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeProgress;
import cn.bugstack.recite.domain.achievement.model.valueobj.UserStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 成就领域服务 — 徽章评估 + 进度计算.
 */
@Slf4j
@Service
public class AchievementService {

    // ================================================================
    // 评估
    // ================================================================

    /** 评估全部 19 枚徽章，返回本次新获得的徽章列表 */
    public List<BadgeDefinition> evaluateAll(UserStatsVO stats) {
        List<BadgeDefinition> newlyEarned = new ArrayList<>();
        Set<String> earned = stats.getEarnedBadgeKeys();

        for (BadgeDefinition badge : BadgeRegistry.ALL_BADGES) {
            if (earned != null && earned.contains(badge.getKey())) {
                continue;
            }
            try {
                if (badge.evaluate(stats)) {
                    newlyEarned.add(badge);
                }
            } catch (Exception e) {
                log.warn("徽章评估异常: badgeKey={}", badge.getKey(), e);
            }
        }

        log.info("徽章评估完成: 已获得={}, 本次新获得={}",
                earned != null ? earned.size() : 0, newlyEarned.size());
        return newlyEarned;
    }

    // ================================================================
    // 进度计算
    // ================================================================

    /** 单枚徽章进度：moduleReciteCount / moduleTotalQuestions */
    public BadgeProgress calculateProgress(BadgeDefinition badge, UserStatsVO stats,
                                           Map<String, Integer> moduleCounts) {
        String key = badge.getKey();
        int count = moduleCounts != null ? moduleCounts.getOrDefault(key, 0) : 0;
        // 目标从 MODULE_TOTAL_QUESTIONS 取，这里用统一阈值 20 作为回退
        int target = 20; // 默认值，实际由前端传入的 moduleReciteCount 决定
        return new BadgeProgress(count, target);
    }
}
