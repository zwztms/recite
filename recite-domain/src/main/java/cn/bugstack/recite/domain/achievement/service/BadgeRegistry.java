package cn.bugstack.recite.domain.achievement.service;

import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeDefinition;
import cn.bugstack.recite.domain.achievement.model.valueobj.UserStatsVO;

import java.util.List;

/**
 * 徽章注册表 — 19 枚模块徽章，每模块一枚.
 *
 * <p>解锁条件：该模块全部题目背诵完成（ recite_count ≥ total_questions ）.
 * 视觉效果：方案A金属渐变 — 金(核心5) / 暗金(Java生态7) / 铜(AI子域4) / 紫金(AI综合3).
 */
public final class BadgeRegistry {

    private BadgeRegistry() {}

    // ================================================================
    // 全部 19 枚 — 按原型四档分类
    // ================================================================

    public static final List<BadgeDefinition> ALL_BADGES = List.of(

            // ========== 金色 · 核心 5 模块 ==========
            badge("jvm", "JVM 虚拟机", "完成 JVM 模块全部 32 题背诵", "JVM",
                    s -> earned(s, "jvm")),
            badge("juc", "JUC 并发", "完成 JUC 模块全部 61 题背诵", "JUC",
                    s -> earned(s, "juc")),
            badge("mysql", "MySQL", "完成 MySQL 模块全部 89 题背诵", "SQL",
                    s -> earned(s, "mysql")),
            badge("redis", "Redis", "完成 Redis 模块全部 43 题背诵", "RED",
                    s -> earned(s, "redis")),
            badge("spring", "Spring", "完成 Spring 模块全部 58 题背诵", "SPR",
                    s -> earned(s, "spring")),

            // ========== 暗金色 · Java 生态 7 模块 ==========
            badge("java-basics", "Java 基础", "完成 Java 基础模块全部 69 题背诵", "JDK",
                    s -> earned(s, "java-basics")),
            badge("java-collections", "集合框架", "完成集合框架模块全部 42 题背诵", "COL",
                    s -> earned(s, "java-collections")),
            badge("os", "操作系统", "完成操作系统模块全部 74 题背诵", "OS",
                    s -> earned(s, "os")),
            badge("ds-algo", "数据结构与算法", "完成数据结构与算法模块全部 33 题背诵", "ALG",
                    s -> earned(s, "ds-algo")),
            badge("network", "计算机网络", "完成计算机网络模块全部 76 题背诵", "NET",
                    s -> earned(s, "network")),
            badge("ai-finetune", "AI 微调", "完成 AI 微调模块全部 21 题背诵", "FT",
                    s -> earned(s, "ai-finetune")),
            badge("ai-openclaw", "OpenClaw", "完成 OpenClaw 模块全部 28 题背诵", "OC",
                    s -> earned(s, "ai-openclaw")),

            // ========== 铜色 · AI 子域 4 模块 ==========
            badge("ai-rag", "AI-RAG", "完成 AI-RAG 模块全部 65 题背诵", "RAG",
                    s -> earned(s, "ai-rag")),
            badge("ai-prompt", "AI Prompt", "完成 AI Prompt 模块全部 6 题背诵", "PRM",
                    s -> earned(s, "ai-prompt")),
            badge("ai-eval", "AI 评估", "完成 AI 评估模块全部 5 题背诵", "EVL",
                    s -> earned(s, "ai-eval")),
            badge("ai-security", "AI 安全", "完成 AI 安全模块全部 3 题背诵", "SEC",
                    s -> earned(s, "ai-security")),

            // ========== 紫金 · AI 综合 3 模块 ==========
            badge("ai-spring", "AI Spring", "完成 AI Spring 模块全部 9 题背诵", "AIS",
                    s -> earned(s, "ai-spring")),
            badge("ai-agent", "AI Agent", "完成 AI Agent 模块全部 19 题背诵", "AGT",
                    s -> earned(s, "ai-agent")),
            badge("ai-design", "AI 设计", "完成 AI 设计模块全部 6 题背诵", "DSG",
                    s -> earned(s, "ai-design"))
    );

    // ---- 辅助 ----

    private static BadgeDefinition badge(String key, String name, String desc, String icon,
                                          java.util.function.Function<UserStatsVO, Boolean> cond) {
        return new BadgeDefinition(key, name, desc, icon, "模块", false, cond);
    }

    private static boolean earned(UserStatsVO s, String key) {
        return s.getEarnedModuleBadges() != null && s.getEarnedModuleBadges().contains(key);
    }

    // ---- 查询 ----

    public static BadgeDefinition getByKey(String key) {
        return ALL_BADGES.stream().filter(b -> b.getKey().equals(key)).findFirst().orElse(null);
    }

    public static List<BadgeDefinition> getPublicBadges() {
        return ALL_BADGES; // 全部公开
    }
}
