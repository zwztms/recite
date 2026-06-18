package cn.bugstack.recite.domain.achievement.service;

import cn.bugstack.recite.domain.achievement.exception.AchievementException;
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

    // ---- 模块 badge key 集合，用于判断 badge 是否为模块类型 ----

    private static final Set<String> MODULE_KEYS = Set.of(
            "java-basics", "juc", "jvm", "java-collections", "spring", "mysql", "redis",
            "os", "ds-algo", "network",
            "ai-rag", "ai-spring", "ai-finetune", "ai-prompt", "ai-eval", "ai-security",
            "ai-design", "ai-openclaw", "ai-agent"
    );

    // ================================================================
    // 评估
    // ================================================================

    /**
     * 评估全部 46 枚徽章，返回本次新获得的徽章列表.
     */
    public List<BadgeDefinition> evaluateAll(UserStatsVO stats) {
        List<BadgeDefinition> newlyEarned = new ArrayList<>();
        Set<String> earned = stats.getEarnedBadgeKeys();

        for (BadgeDefinition badge : BadgeRegistry.ALL_BADGES) {
            if (earned != null && earned.contains(badge.getKey())) {
                continue; // 已获得
            }
            try {
                if (badge.evaluate(stats)) {
                    newlyEarned.add(badge);
                }
            } catch (Exception e) {
                log.warn("徽章评估异常: badgeKey={}, userId 见上下文", badge.getKey(), e);
            }
        }

        log.info("徽章评估完成: 已获得={}, 本次新获得={}",
                earned != null ? earned.size() : 0, newlyEarned.size());
        return newlyEarned;
    }

    // ================================================================
    // 进度计算
    // ================================================================

    /**
     * 计算单枚徽章进度.
     *
     * <p>模块徽章需传入 moduleCounts（模块→背诵次数），非模块徽章忽略该参数.</p>
     */
    public BadgeProgress calculateProgress(BadgeDefinition badge, UserStatsVO stats,
                                           Map<String, Integer> moduleCounts) {
        String key = badge.getKey();

        // -- 背诵量 --
        if (key.startsWith("total_")) {
            int target = Integer.parseInt(key.substring(6)); // total_100 → 100
            return new BadgeProgress(stats.getTotalRecites(), target);
        }
        // -- 质量 avg --
        if (key.startsWith("avg_")) {
            int target = Integer.parseInt(key.substring(4)); // avg_7 → 7
            return new BadgeProgress((int) (stats.getAverageScore() * 10), target * 10);
        }
        // -- 质量 perfect --
        if (key.startsWith("perfect_")) {
            int target = Integer.parseInt(key.substring(8)); // perfect_10 → 10
            return new BadgeProgress(stats.getPerfectScoreCount(), target);
        }
        // -- 坚持 --
        if (key.startsWith("streak_")) {
            int target = Integer.parseInt(key.substring(7)); // streak_7 → 7
            return new BadgeProgress(stats.getCurrentStreak(), target);
        }
        // -- 模块 --
        if (MODULE_KEYS.contains(key)) {
            int count = moduleCounts != null ? moduleCounts.getOrDefault(key, 0) : 0;
            return new BadgeProgress(count, 20);
        }
        // -- 组合 --
        if (key.startsWith("combo_")) {
            return computeComboProgress(key, stats);
        }
        // -- 隐藏（不展示进度） --
        return new BadgeProgress(0, 1);
    }

    /** 组合徽章进度 */
    private BadgeProgress computeComboProgress(String key, UserStatsVO stats) {
        Set<String> earned = stats.getEarnedModuleBadges();
        if (earned == null) earned = Set.of();
        return switch (key) {
            case "combo_all_java" -> {
                Set<String> req = Set.of("java-basics", "juc", "jvm", "java-collections");
                long c = req.stream().filter(earned::contains).count();
                yield new BadgeProgress((int) c, 4);
            }
            case "combo_all_ai" -> {
                Set<String> req = Set.of("ai-rag", "ai-spring", "ai-finetune", "ai-prompt",
                        "ai-eval", "ai-security", "ai-design", "ai-openclaw", "ai-agent");
                long c = req.stream().filter(earned::contains).count();
                yield new BadgeProgress((int) c, 9);
            }
            case "combo_all_cs" -> {
                Set<String> req = Set.of("os", "ds-algo", "network");
                long c = req.stream().filter(earned::contains).count();
                yield new BadgeProgress((int) c, 3);
            }
            case "combo_all_modules" -> {
                long c = MODULE_KEYS.stream().filter(earned::contains).count();
                yield new BadgeProgress((int) c, 19);
            }
            default -> new BadgeProgress(0, 1);
        };
    }
}
