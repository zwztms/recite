package cn.bugstack.recite.domain.achievement;

import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeDefinition;
import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeProgress;
import cn.bugstack.recite.domain.achievement.model.valueobj.UserStatsVO;
import cn.bugstack.recite.domain.achievement.service.AchievementService;
import cn.bugstack.recite.domain.achievement.service.BadgeRegistry;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("成就徽章系统")
class AchievementServiceTest {

    private AchievementService service;

    @BeforeEach
    void setUp() {
        service = new AchievementService();
    }

    // ---- helper ----

    private static UserStatsVO stats(int totalRecites, double avgScore, int perfectCount,
                                      int currentStreak, int longestStreak,
                                      int masteredCount, int totalSessions,
                                      Set<String> earnedModuleBadges, Set<String> earnedBadgeKeys) {
        return new UserStatsVO(totalRecites, avgScore, perfectCount,
                currentStreak, longestStreak, masteredCount, totalSessions,
                earnedModuleBadges != null ? earnedModuleBadges : Set.of(),
                earnedBadgeKeys != null ? earnedBadgeKeys : Set.of(),
                0, 0, 0, 0, 0, false);
    }

    // ================================================================
    // evaluateAll
    // ================================================================

    @Nested
    @DisplayName("evaluateAll — 背诵量")
    class EvaluateVolume {

        @Test
        @DisplayName("total_10: 10 题 → 获得")
        void total10ShouldEarn() {
            var s = stats(10, 0, 0, 0, 0, 0, 0, Set.of(), Set.of());
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("total_10");
        }

        @Test
        @DisplayName("total_100: 101 题 → 获得百题斩")
        void total100ShouldEarn() {
            var s = stats(101, 0, 0, 0, 0, 0, 0, Set.of(), Set.of());
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("total_10", "total_50", "total_100");
        }

        @Test
        @DisplayName("total_500: 满额 → 全部背诵量徽章 + 隐藏")
        void total500AllVolume() {
            var s = stats(500, 0, 0, 0, 0, 0, 1, Set.of(), Set.of());
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key")
                    .contains("total_10", "total_50", "total_100", "total_300", "total_500")
                    .contains("hidden_first_blood"); // 有会话记录
        }
    }

    @Nested
    @DisplayName("evaluateAll — 质量")
    class EvaluateQuality {

        @Test
        @DisplayName("avg_9: 均分 9.2 → 获得")
        void highAvgShouldEarn() {
            var s = stats(50, 9.2, 0, 0, 0, 0, 0, Set.of(), Set.of());
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("avg_9");
        }

        @Test
        @DisplayName("perfect_1: 首战满分 → 获得")
        void firstPerfectShouldEarn() {
            var s = stats(5, 6.0, 1, 0, 0, 0, 0, Set.of(), Set.of());
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("perfect_1");
        }
    }

    @Nested
    @DisplayName("evaluateAll — 坚持")
    class EvaluateStreak {

        @Test
        @DisplayName("streak_7: 连续 7 天 → 获得")
        void streak7ShouldEarn() {
            var s = stats(70, 7.0, 0, 7, 7, 0, 0, Set.of(), Set.of());
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("streak_3", "streak_7");
        }

        @Test
        @DisplayName("已获得不重复发放")
        void alreadyEarnedShouldNotRepeat() {
            var s = stats(101, 0, 0, 0, 0, 0, 0, Set.of(),
                    Set.of("total_10", "total_50", "total_100"));
            List<BadgeDefinition> badges = service.evaluateAll(s);
            // total_10/50/100 已获得，不应再出现
            assertThat(badges).extracting("key")
                    .doesNotContain("total_10", "total_50", "total_100");
        }
    }

    @Nested
    @DisplayName("evaluateAll — 模块")
    class EvaluateModule {

        @Test
        @DisplayName("集齐 Java 4 模块 → combo_all_java")
        void javaComboShouldEarn() {
            var s = stats(80, 7.0, 0, 0, 0, 0, 0,
                    Set.of("java-basics", "juc", "jvm", "java-collections"),
                    Set.of("java-basics", "juc", "jvm", "java-collections"));
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("combo_all_java");
        }

        @Test
        @DisplayName("模块徽章依赖 earnedModuleBadges")
        void moduleBadgeNeedsEarnedModules() {
            // 已获得 jvm 模块徽章，但 earnModuleBadges 中没有 → 组合不触发
            var s = stats(80, 7.0, 0, 0, 0, 0, 0,
                    Set.of("jvm"), // 只有 1 个
                    Set.of("jvm"));
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key")
                    .doesNotContain("combo_all_java");
        }
    }

    @Nested
    @DisplayName("evaluateAll — 隐藏")
    class EvaluateHidden {

        @Test
        @DisplayName("夜猫子: 凌晨 0-5 点 → 获得")
        void nightOwlShouldEarn() {
            var s = new UserStatsVO(10, 5.0, 0, 0, 0, 0, 1,
                    Set.of(), Set.of(), 3, 0, 0, 0, 0, false);
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("hidden_night_owl");
        }

        @Test
        @DisplayName("第一滴血: 1 次会话 → 获得")
        void firstBloodShouldEarn() {
            var s = stats(3, 5.0, 0, 0, 0, 0, 1, Set.of(), Set.of());
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("hidden_first_blood");
        }

        @Test
        @DisplayName("狙击手: 追问 3 层 → 获得")
        void sniperShouldEarn() {
            var s = new UserStatsVO(10, 5.0, 0, 0, 0, 0, 1,
                    Set.of(), Set.of(), 12, 0, 0, 0, 3, false);
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges).extracting("key").contains("hidden_sniper");
        }
    }

    @Nested
    @DisplayName("evaluateAll — 一次多枚")
    class MultipleBadges {

        @Test
        @DisplayName("全满统计 → 应获得 30+ 枚")
        void godModeShouldEarnMany() {
            var s = new UserStatsVO(500, 9.5, 10, 60, 60, 100, 50,
                    Set.of("java-basics", "juc", "jvm", "java-collections",
                            "spring", "mysql", "redis", "os", "ds-algo", "network",
                            "ai-rag", "ai-spring", "ai-finetune", "ai-prompt",
                            "ai-eval", "ai-security", "ai-design", "ai-openclaw", "ai-agent"),
                    Set.of(), 1, 25, 3, 20, 3, false);
            List<BadgeDefinition> badges = service.evaluateAll(s);
            assertThat(badges.size()).isGreaterThanOrEqualTo(30);
        }
    }

    // ================================================================
    // calculateProgress
    // ================================================================

    @Nested
    @DisplayName("calculateProgress")
    class CalculateProgress {

        @Test
        @DisplayName("total_100: 67/100 → 67%")
        void volumeProgress() {
            var s = stats(67, 0, 0, 0, 0, 0, 0, Set.of(), Set.of());
            BadgeDefinition badge = BadgeRegistry.getByKey("total_100");
            BadgeProgress p = service.calculateProgress(badge, s, Map.of());
            assertThat(p.getCurrent()).isEqualTo(67);
            assertThat(p.getTarget()).isEqualTo(100);
            assertThat(p.getPercent()).isEqualTo(67);
        }

        @Test
        @DisplayName("avg_8: 均分 6.5 → (65/80) = 81%")
        void avgProgress() {
            var s = stats(10, 6.5, 0, 0, 0, 0, 0, Set.of(), Set.of());
            BadgeDefinition badge = BadgeRegistry.getByKey("avg_8");
            BadgeProgress p = service.calculateProgress(badge, s, Map.of());
            assertThat(p.getCurrent()).isEqualTo(65); // 6.5*10
            assertThat(p.getTarget()).isEqualTo(80);   // 8*10
        }

        @Test
        @DisplayName("streak_7: 当前连续 3 天 → 3/7 → 42%")
        void streakProgress() {
            var s = stats(0, 0, 0, 3, 3, 0, 0, Set.of(), Set.of());
            BadgeDefinition badge = BadgeRegistry.getByKey("streak_7");
            BadgeProgress p = service.calculateProgress(badge, s, Map.of());
            assertThat(p.getCurrent()).isEqualTo(3);
            assertThat(p.getTarget()).isEqualTo(7);
            assertThat(p.getPercent()).isEqualTo(42);
        }

        @Test
        @DisplayName("jvm 模块: 模块次数 12 → 12/20 → 60%")
        void moduleProgress() {
            var s = stats(12, 6.0, 0, 0, 0, 0, 0, Set.of(), Set.of());
            BadgeDefinition badge = BadgeRegistry.getByKey("jvm");
            BadgeProgress p = service.calculateProgress(badge, s,
                    Map.of("jvm", 12));
            assertThat(p.getCurrent()).isEqualTo(12);
            assertThat(p.getTarget()).isEqualTo(20);
            assertThat(p.getPercent()).isEqualTo(60);
        }

        @Test
        @DisplayName("combo_all_java: 已获 2/4 → 50%")
        void comboProgress() {
            var s = stats(0, 0, 0, 0, 0, 0, 0,
                    Set.of("java-basics", "jvm"), Set.of());
            BadgeDefinition badge = BadgeRegistry.getByKey("combo_all_java");
            BadgeProgress p = service.calculateProgress(badge, s, Map.of());
            assertThat(p.getCurrent()).isEqualTo(2);
            assertThat(p.getTarget()).isEqualTo(4);
            assertThat(p.getPercent()).isEqualTo(50);
        }

        @Test
        @DisplayName("隐藏徽章进度 = 0/1")
        void hiddenProgress() {
            var s = stats(0, 0, 0, 0, 0, 0, 0, Set.of(), Set.of());
            BadgeDefinition badge = BadgeRegistry.getByKey("hidden_night_owl");
            BadgeProgress p = service.calculateProgress(badge, s, Map.of());
            assertThat(p.getCurrent()).isEqualTo(0);
            assertThat(p.getTarget()).isEqualTo(1);
        }
    }

    // ================================================================
    // BadgeRegistry 基础验证
    // ================================================================

    @Nested
    @DisplayName("BadgeRegistry 注册表")
    class RegistryValidation {

        @Test
        @DisplayName("全部 46 枚定义")
        void shouldHave46Badges() {
            assertThat(BadgeRegistry.ALL_BADGES).hasSize(46);
        }

        @Test
        @DisplayName("按分类过滤")
        void shouldFilterByCategory() {
            assertThat(BadgeRegistry.getByCategory("质量")).hasSize(5);
            assertThat(BadgeRegistry.getByCategory("坚持")).hasSize(5);
            assertThat(BadgeRegistry.getByCategory("背诵量")).hasSize(5);
            assertThat(BadgeRegistry.getByCategory("模块")).hasSize(19);
            assertThat(BadgeRegistry.getByCategory("组合")).hasSize(4);
            assertThat(BadgeRegistry.getByCategory("隐藏")).hasSize(8);
        }

        @Test
        @DisplayName("隐藏徽章 8 枚")
        void shouldHave8Hidden() {
            long hidden = BadgeRegistry.ALL_BADGES.stream()
                    .filter(BadgeDefinition::isHidden).count();
            assertThat(hidden).isEqualTo(8);
        }

        @Test
        @DisplayName("getPublicBadges 排除隐藏 = 38 枚")
        void publicBadgesShouldExcludeHidden() {
            assertThat(BadgeRegistry.getPublicBadges()).hasSize(38);
        }
    }
}
