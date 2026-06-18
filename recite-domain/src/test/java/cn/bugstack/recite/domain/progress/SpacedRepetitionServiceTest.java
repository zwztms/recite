package cn.bugstack.recite.domain.progress;

import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
import cn.bugstack.recite.domain.progress.service.SpacedRepetitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("间隔重复算法")
class SpacedRepetitionServiceTest {

    private SpacedRepetitionService service;

    @BeforeEach
    void setUp() {
        service = new SpacedRepetitionService();
    }

    // ================================================================
    // 首次背诵
    // ================================================================

    @Nested
    @DisplayName("首次背诵")
    class FirstRecitation {

        @Test
        @DisplayName("应设置初始interval=1 ease=2.5 mastery=score*10")
        void shouldSetInitialValues() {
            var result = service.calculateAfterScore(null, 7, 1L, "q1", "jvm");

            assertThat(result.getReviewInterval()).isEqualTo(1);
            assertThat(result.getEaseFactor()).isEqualTo(2.5);
            assertThat(result.getMasteryScore()).isEqualTo(70);
            assertThat(result.getReciteCount()).isEqualTo(1);
            assertThat(result.getAverageScore()).isEqualTo(7.0);
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getQuestionId()).isEqualTo("q1");
            assertThat(result.getModuleKey()).isEqualTo("jvm");
            assertThat(result.getNextReviewAt()).isNotNull();
            assertThat(result.getLastRecitedAt()).isNotNull();
        }

        @Test
        @DisplayName("满分10分 → mastery=100")
        void perfectScoreShouldGiveFullMastery() {
            var result = service.calculateAfterScore(null, 10, 1L, "q1", "jvm");
            assertThat(result.getMasteryScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("低分1分 → mastery=10")
        void lowScoreShouldGiveLowMastery() {
            var result = service.calculateAfterScore(null, 1, 1L, "q1", "jvm");
            assertThat(result.getMasteryScore()).isEqualTo(10);
        }
    }

    // ================================================================
    // 分数区间行为
    // ================================================================

    @Nested
    @DisplayName("高分 ≥8")
    class HighScore {

        @Test
        @DisplayName("应拉大间隔 interval × ease")
        void shouldIncreaseInterval() {
            var current = createProgress(3, 2.5, 50, 5);
            var result = service.calculateAfterScore(current, 9, 1L, "q1", "jvm");

            // interval = round(3 * 2.5) = 8
            assertThat(result.getReviewInterval()).isEqualTo(8);
            // ease = 2.5 + 0.1 = 2.6
            assertThat(result.getEaseFactor()).isEqualTo(2.6);
        }

        @Test
        @DisplayName("ease 上限不应超过 5.0")
        void easeShouldNotExceedMax() {
            var current = createProgress(10, 4.95, 80, 3);
            var result = service.calculateAfterScore(current, 9, 1L, "q1", "jvm");

            assertThat(result.getEaseFactor()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("interval 上限不应超过 365")
        void intervalShouldNotExceed365() {
            var current = createProgress(300, 5.0, 90, 10);
            var result = service.calculateAfterScore(current, 9, 1L, "q1", "jvm");

            // round(300 * 5.0) = 1500 → clamp to 365
            assertThat(result.getReviewInterval()).isEqualTo(365);
        }
    }

    @Nested
    @DisplayName("中等分 5-7")
    class MediumScore {

        @Test
        @DisplayName("interval 应保持不变，ease 不变")
        void shouldKeepIntervalAndEase() {
            var current = createProgress(7, 2.5, 60, 4);
            var result = service.calculateAfterScore(current, 6, 1L, "q1", "jvm");

            assertThat(result.getReviewInterval()).isEqualTo(7);
            assertThat(result.getEaseFactor()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("分数5 边界")
        void scoreBoundary5() {
            var current = createProgress(5, 2.0, 50, 3);
            var result = service.calculateAfterScore(current, 5, 1L, "q1", "jvm");

            assertThat(result.getReviewInterval()).isEqualTo(5);
            assertThat(result.getEaseFactor()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("低分 ≤4")
    class LowScore {

        @Test
        @DisplayName("应重置 interval=1，降低 ease")
        void shouldResetIntervalAndDecreaseEase() {
            var current = createProgress(14, 3.0, 80, 10);
            var result = service.calculateAfterScore(current, 3, 1L, "q1", "jvm");

            assertThat(result.getReviewInterval()).isEqualTo(1);
            assertThat(result.getEaseFactor()).isEqualTo(2.8); // 3.0 - 0.2
        }

        @Test
        @DisplayName("ease 下限不应低于 1.3")
        void easeShouldNotFallBelowMin() {
            var current = createProgress(3, 1.4, 40, 5);
            var result = service.calculateAfterScore(current, 2, 1L, "q1", "jvm");

            assertThat(result.getEaseFactor()).isEqualTo(1.3);
        }
    }

    // ================================================================
    // 掌握度加权
    // ================================================================

    @Nested
    @DisplayName("掌握度加权")
    class MasteryWeighting {

        @Test
        @DisplayName("新掌握度 = 旧×0.7 + 本次×0.3")
        void shouldWeightCorrectly() {
            var current = createProgress(5, 2.5, 80, 8); // 旧 mastery=80
            var result = service.calculateAfterScore(current, 5, 1L, "q1", "jvm");

            // newMastery = 80*0.7 + 50*0.3 = 56 + 15 = 71
            assertThat(result.getMasteryScore()).isEqualTo(71);
        }

        @Test
        @DisplayName("掌握度不应低于 0")
        void masteryShouldNotFallBelowZero() {
            var current = createProgress(1, 2.5, 0, 3);
            var result = service.calculateAfterScore(current, 1, 1L, "q1", "jvm");

            assertThat(result.getMasteryScore()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("掌握度不应超过 100")
        void masteryShouldNotExceed100() {
            var current = createProgress(10, 3.0, 99, 12);
            var result = service.calculateAfterScore(current, 10, 1L, "q1", "jvm");

            assertThat(result.getMasteryScore()).isLessThanOrEqualTo(100);
        }
    }

    // ================================================================
    // 连续背诵
    // ================================================================

    @Nested
    @DisplayName("连续多次背诵")
    class ConsecutiveRecitations {

        @Test
        @DisplayName("连续高分应使间隔以指数增长")
        void consecutiveHighScoresShouldGrowIntervalExponentially() {
            var r1 = service.calculateAfterScore(null, 9, 1L, "q1", "jvm");
            assertThat(r1.getReviewInterval()).isEqualTo(1);

            var r2 = service.calculateAfterScore(r1, 9, 1L, "q1", "jvm");
            // interval = round(1 * 2.6) = 3
            assertThat(r2.getReviewInterval()).isEqualTo(3);
            assertThat(r2.getEaseFactor()).isEqualTo(2.6);

            var r3 = service.calculateAfterScore(r2, 9, 1L, "q1", "jvm");
            // interval = round(3 * 2.7) = 8
            assertThat(r3.getReviewInterval()).isEqualTo(8);
            assertThat(r3.getEaseFactor()).isEqualTo(2.7);
        }

        @Test
        @DisplayName("高分→低分→高分：间隔先降后升")
        void mixedScoresShouldAdjustCorrectly() {
            var r1 = service.calculateAfterScore(null, 9, 1L, "q1", "jvm");
            var r2 = service.calculateAfterScore(r1, 2, 1L, "q1", "jvm"); // 低分重置
            assertThat(r2.getReviewInterval()).isEqualTo(1);

            var r3 = service.calculateAfterScore(r2, 8, 1L, "q1", "jvm"); // 再次高分
            // ease was 2.5 + 0.1 - 0.2 + 0.1... wait let me trace:
            // r1 (null→9): interval=1, ease=2.6
            // r2 (score 2): interval=1, ease=2.4
            // r3 (score 8): interval=round(1 * 2.4)=2, ease=2.5
            assertThat(r3.getReviewInterval()).isGreaterThan(1);
            // ease: 2.6(r1) → 2.4(r2,低分降0.2) → 2.5(r3,高分升0.1)
            assertThat(r3.getEaseFactor()).isGreaterThanOrEqualTo(2.4);
            assertThat(r2.getEaseFactor()).isLessThan(r1.getEaseFactor());  // 低分应降 ease
        }
    }

    // ================================================================
    // 辅助
    // ================================================================

    private UserProgressEntity createProgress(int interval, double ease, int mastery, int count) {
        return UserProgressEntity.builder()
                .id(1L).userId(1L).questionId("q1").moduleKey("jvm")
                .reciteCount(count).masteryScore(mastery).averageScore(7.0)
                .reviewInterval(interval).easeFactor(ease)
                .lastRecitedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
