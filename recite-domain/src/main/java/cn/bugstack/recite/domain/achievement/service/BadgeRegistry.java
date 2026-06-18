package cn.bugstack.recite.domain.achievement.service;

import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeDefinition;
import cn.bugstack.recite.domain.achievement.model.valueobj.UserStatsVO;

import java.util.List;
import java.util.Set;

/**
 * 徽章注册表 — 全部 46 枚徽章硬编码定义.
 *
 * <p>分类：背诵量(5) + 质量(5) + 坚持(5) + 模块(19) + 组合(4) + 隐藏(8).</p>
 */
public final class BadgeRegistry {

    private BadgeRegistry() {}

    // ---- 模块列表 ----

    private static final Set<String> JAVA_MODULES = Set.of("java-basics", "juc", "jvm", "java-collections");
    private static final Set<String> AI_MODULES = Set.of("ai-rag", "ai-spring", "ai-finetune", "ai-prompt",
            "ai-eval", "ai-security", "ai-design", "ai-openclaw", "ai-agent");
    private static final Set<String> CS_MODULES = Set.of("os", "ds-algo", "network");
    private static final List<String> ALL_19_MODULES = List.of(
            "java-basics", "juc", "jvm", "java-collections", "spring", "mysql", "redis",
            "os", "ds-algo", "network",
            "ai-rag", "ai-spring", "ai-finetune", "ai-prompt", "ai-eval", "ai-security",
            "ai-design", "ai-openclaw", "ai-agent"
    );

    // ================================================================
    // 全部 46 枚
    // ================================================================

    public static final List<BadgeDefinition> ALL_BADGES = List.of(

            // ========== 背诵量（5） ==========
            new BadgeDefinition("total_10", "初出茅庐", "累计背诵 10 题", "total_10",
                    "背诵量", false, s -> s.getTotalRecites() >= 10),
            new BadgeDefinition("total_50", "小有所成", "累计背诵 50 题", "total_50",
                    "背诵量", false, s -> s.getTotalRecites() >= 50),
            new BadgeDefinition("total_100", "百题斩", "累计背诵 100 题", "total_100",
                    "背诵量", false, s -> s.getTotalRecites() >= 100),
            new BadgeDefinition("total_300", "题海勇士", "累计背诵 300 题", "total_300",
                    "背诵量", false, s -> s.getTotalRecites() >= 300),
            new BadgeDefinition("total_500", "题库终结者", "累计背诵 500 题", "total_500",
                    "背诵量", false, s -> s.getTotalRecites() >= 500),

            // ========== 质量（5） ==========
            new BadgeDefinition("avg_7", "稳定发挥", "历史平均分 ≥ 7", "avg_7",
                    "质量", false, s -> s.getAverageScore() >= 7.0),
            new BadgeDefinition("avg_8", "优秀学者", "历史平均分 ≥ 8", "avg_8",
                    "质量", false, s -> s.getAverageScore() >= 8.0),
            new BadgeDefinition("avg_9", "接近完美", "历史平均分 ≥ 9", "avg_9",
                    "质量", false, s -> s.getAverageScore() >= 9.0),
            new BadgeDefinition("perfect_1", "首战满分", "首次获得 10 分", "perfect_1",
                    "质量", false, s -> s.getPerfectScoreCount() >= 1),
            new BadgeDefinition("perfect_10", "满分收割机", "累计 10 次 10 分", "perfect_10",
                    "质量", false, s -> s.getPerfectScoreCount() >= 10),

            // ========== 坚持（5） ==========
            new BadgeDefinition("streak_3", "三天打鱼", "连续 3 天背诵", "streak_3",
                    "坚持", false, s -> s.getCurrentStreak() >= 3),
            new BadgeDefinition("streak_7", "周不懈怠", "连续 7 天背诵", "streak_7",
                    "坚持", false, s -> s.getCurrentStreak() >= 7),
            new BadgeDefinition("streak_14", "半月坚持", "连续 14 天背诵", "streak_14",
                    "坚持", false, s -> s.getCurrentStreak() >= 14),
            new BadgeDefinition("streak_30", "月度之星", "连续 30 天背诵", "streak_30",
                    "坚持", false, s -> s.getCurrentStreak() >= 30),
            new BadgeDefinition("streak_60", "持之以恒", "连续 60 天背诵", "streak_60",
                    "坚持", false, s -> s.getCurrentStreak() >= 60),

            // ========== 模块单枚（19） ==========
            new BadgeDefinition("java-basics", "Java 基础达人", "Java 基础模块背诵 ≥ 20 题",
                    "module_java", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("java-basics")),
            new BadgeDefinition("juc", "并发大神", "JUC 模块背诵 ≥ 20 题",
                    "module_juc", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("juc")),
            new BadgeDefinition("jvm", "JVM 专家", "JVM 模块背诵 ≥ 20 题",
                    "module_jvm", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("jvm")),
            new BadgeDefinition("java-collections", "集合大师", "Java 集合模块背诵 ≥ 20 题",
                    "module_collections", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("java-collections")),
            new BadgeDefinition("spring", "Spring 高手", "Spring 模块背诵 ≥ 20 题",
                    "module_spring", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("spring")),
            new BadgeDefinition("mysql", "MySQL 达人", "MySQL 模块背诵 ≥ 20 题",
                    "module_mysql", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("mysql")),
            new BadgeDefinition("redis", "Redis 达人", "Redis 模块背诵 ≥ 20 题",
                    "module_redis", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("redis")),
            new BadgeDefinition("os", "操作系统通", "操作系统模块背诵 ≥ 20 题",
                    "module_os", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("os")),
            new BadgeDefinition("ds-algo", "算法高手", "数据结构与算法模块背诵 ≥ 20 题",
                    "module_ds", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ds-algo")),
            new BadgeDefinition("network", "网络专家", "计算机网络模块背诵 ≥ 20 题",
                    "module_network", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("network")),
            new BadgeDefinition("ai-rag", "RAG 先锋", "AI-RAG 模块背诵 ≥ 20 题",
                    "module_ai_rag", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-rag")),
            new BadgeDefinition("ai-spring", "AI Spring 达人", "AI Spring 模块背诵 ≥ 20 题",
                    "module_ai_spring", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-spring")),
            new BadgeDefinition("ai-finetune", "微调大师", "AI 微调模块背诵 ≥ 20 题",
                    "module_ai_finetune", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-finetune")),
            new BadgeDefinition("ai-prompt", "Prompt 专家", "AI Prompt 模块背诵 ≥ 20 题",
                    "module_ai_prompt", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-prompt")),
            new BadgeDefinition("ai-eval", "评估达人", "AI 评估模块背诵 ≥ 20 题",
                    "module_ai_eval", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-eval")),
            new BadgeDefinition("ai-security", "安全卫士", "AI 安全模块背诵 ≥ 20 题",
                    "module_ai_security", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-security")),
            new BadgeDefinition("ai-design", "架构设计师", "AI 设计模块背诵 ≥ 20 题",
                    "module_ai_design", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-design")),
            new BadgeDefinition("ai-openclaw", "OpenClaw 专家", "AI OpenClaw 模块背诵 ≥ 20 题",
                    "module_ai_openclaw", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-openclaw")),
            new BadgeDefinition("ai-agent", "Agent 专家", "AI Agent 模块背诵 ≥ 20 题",
                    "module_ai_agent", "模块", false,
                    s -> s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains("ai-agent")),

            // ========== 组合（4） ==========
            new BadgeDefinition("combo_all_java", "Java 全栈", "集齐 java-basics + juc + jvm + java-collections",
                    "combo_java", "组合", false,
                    s -> s.getEarnedModuleBadges() != null
                            && s.getEarnedModuleBadges().containsAll(JAVA_MODULES)),
            new BadgeDefinition("combo_all_ai", "AI 专家", "集齐全部 9 个 AI 模块",
                    "combo_ai", "组合", false,
                    s -> s.getEarnedModuleBadges() != null
                            && s.getEarnedModuleBadges().containsAll(AI_MODULES)),
            new BadgeDefinition("combo_all_cs", "计算机基础", "集齐 os + ds-algo + network",
                    "combo_cs", "组合", false,
                    s -> s.getEarnedModuleBadges() != null
                            && s.getEarnedModuleBadges().containsAll(CS_MODULES)),
            new BadgeDefinition("combo_all_modules", "全模块制霸", "集齐全部 19 个模块",
                    "combo_all", "组合", false,
                    s -> s.getEarnedModuleBadges() != null
                            && s.getEarnedModuleBadges().containsAll(ALL_19_MODULES)),

            // ========== 趣味隐藏（8） ==========
            new BadgeDefinition("hidden_night_owl", "夜猫子", "在凌晨 0-5 点完成一次背诵",
                    "hidden_night", "隐藏", true,
                    s -> s.getLastSessionHour() >= 0 && s.getLastSessionHour() < 5),
            new BadgeDefinition("hidden_speed", "快枪手", "单题答题 ≤ 30 秒",
                    "hidden_speed", "隐藏", true,
                    s -> s.getLastSessionAnswerSeconds() > 0 && s.getLastSessionAnswerSeconds() <= 30),
            new BadgeDefinition("hidden_three_stars", "三星上将", "单次会话 3 题全满分",
                    "hidden_three", "隐藏", true,
                    s -> s.getLastSessionPerfectCount() >= 3
                            && s.getLastSessionPerfectCount() == s.getLastSessionQuestionCount()
                            && s.getLastSessionQuestionCount() >= 3),
            new BadgeDefinition("hidden_comeback", "卷土重来", "断签 7 天后再次连续 3 天",
                    "hidden_comeback", "隐藏", true,
                    s -> s.isWasStreakBroken() && s.getCurrentStreak() >= 3),
            new BadgeDefinition("hidden_first_blood", "第一滴血", "第一次完成任何背诵",
                    "hidden_first", "隐藏", true,
                    s -> s.getTotalSessions() >= 1),
            new BadgeDefinition("hidden_marathon", "马拉松", "单次会话 ≥ 20 题",
                    "hidden_marathon", "隐藏", true,
                    s -> s.getLastSessionQuestionCount() >= 20),
            new BadgeDefinition("hidden_sniper", "狙击手", "追问链达到 3 层",
                    "hidden_sniper", "隐藏", true,
                    s -> s.getLastSessionMaxFollowUpDepth() >= 3),
            new BadgeDefinition("hidden_collector", "收藏家", "获得 30 枚徽章",
                    "hidden_collector", "隐藏", true,
                    s -> s.getEarnedBadgeKeys() != null && s.getEarnedBadgeKeys().size() >= 30)
    );

    // ---- 查询方法 ----

    public static BadgeDefinition getByKey(String key) {
        return ALL_BADGES.stream()
                .filter(b -> b.getKey().equals(key))
                .findFirst().orElse(null);
    }

    public static List<BadgeDefinition> getByCategory(String category) {
        return ALL_BADGES.stream()
                .filter(b -> b.getCategory().equals(category))
                .toList();
    }

    /** 公开徽章（排除隐藏） */
    public static List<BadgeDefinition> getPublicBadges() {
        return ALL_BADGES.stream()
                .filter(b -> !b.isHidden())
                .toList();
    }
}
