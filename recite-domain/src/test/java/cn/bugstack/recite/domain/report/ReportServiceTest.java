package cn.bugstack.recite.domain.report;

import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.port.out.LlmPort;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.domain.report.exception.ReportException;
import cn.bugstack.recite.domain.report.model.entity.LearningJournal;
import cn.bugstack.recite.domain.report.port.out.ReportPort;
import cn.bugstack.recite.domain.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("报告服务")
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReciteRecordPort reciteRecordPort;
    @Mock private ProgressPort progressPort;
    @Mock private StreakPort streakPort;
    @Mock private ReportPort reportPort;
    @Mock private LlmPort llmPort;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(reciteRecordPort, progressPort,
                streakPort, reportPort, llmPort);
    }

    @Nested
    @DisplayName("aggregateDashboard")
    class AggregateDashboard {

        @Test
        @DisplayName("空数据 → 各项为 0 或空")
        void emptyDataShouldReturnZeros() {
            when(progressPort.findByUserId(1L)).thenReturn(List.of());
            when(streakPort.findByUserId(1L)).thenReturn(Optional.empty());
            when(reportPort.countByUserId(1L)).thenReturn(0);
            when(reportPort.findRecentJournals(1L, 7)).thenReturn(List.of());

            var result = service.aggregateDashboard(1L);

            assertThat(result.statsCard().totalQuestions()).isZero();
            assertThat(result.statsCard().avgMastery()).isZero();
            assertThat(result.statsCard().streakDays()).isZero();
            assertThat(result.statsCard().reportCount()).isZero();
            assertThat(result.moduleBars()).isEmpty();
            assertThat(result.trend()).isEmpty();
            assertThat(result.weakTags()).isEmpty();
            assertThat(result.latestAdvice()).isEmpty();
        }

        @Test
        @DisplayName("有进度数据 → 统计正确")
        void progressDataShouldAggregateCorrectly() {
            var p1 = createProgress(5, 80, "jvm");
            var p2 = createProgress(3, 60, "juc");
            when(progressPort.findByUserId(1L)).thenReturn(List.of(p1, p2));
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(
                    UserStreakEntity.builder().userId(1L).currentStreak(7)
                            .lastActiveDate(java.time.LocalDate.now()).longestStreak(10).build()));
            when(reportPort.countByUserId(1L)).thenReturn(3);
            when(reportPort.findRecentJournals(1L, 7)).thenReturn(List.of());

            var result = service.aggregateDashboard(1L);

            assertThat(result.statsCard().totalQuestions()).isEqualTo(8); // 5+3
            assertThat(result.statsCard().avgMastery()).isEqualTo(70.0);  // (80+60)/2
            assertThat(result.statsCard().streakDays()).isEqualTo(7);
            assertThat(result.statsCard().reportCount()).isEqualTo(3);
            assertThat(result.moduleBars()).hasSize(2);
            assertThat(result.moduleBars()).containsEntry("jvm", 80.0);
            assertThat(result.moduleBars()).containsEntry("juc", 60.0);
        }

        @Test
        @DisplayName("无连续数据 → streak=0")
        void noStreakShouldReturnZero() {
            when(progressPort.findByUserId(1L)).thenReturn(List.of());
            when(streakPort.findByUserId(1L)).thenReturn(Optional.empty());
            when(reportPort.countByUserId(1L)).thenReturn(0);
            when(reportPort.findRecentJournals(1L, 7)).thenReturn(List.of());

            var result = service.aggregateDashboard(1L);
            assertThat(result.statsCard().streakDays()).isZero();
        }

        @Test
        @DisplayName("有学习档案 → 提取薄弱标签和最新建议")
        void journalsShouldExtractTagsAndAdvice() {
            when(progressPort.findByUserId(1L)).thenReturn(List.of());
            when(streakPort.findByUserId(1L)).thenReturn(Optional.empty());
            when(reportPort.countByUserId(1L)).thenReturn(0);

            var journal = LearningJournal.builder()
                    .id(1L).userId(1L).sessionId("s1")
                    .summaryJson("{\"weakTags\":[\"JVM\",\"线程安全\"],\"advice\":\"多练习并发\"}")
                    .createdAt(LocalDateTime.now()).build();
            when(reportPort.findRecentJournals(1L, 7)).thenReturn(List.of(journal));

            var result = service.aggregateDashboard(1L);

            assertThat(result.weakTags()).contains("JVM", "线程安全");
            assertThat(result.latestAdvice()).isEqualTo("多练习并发");
            assertThat(result.trend()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("generateReport")
    class GenerateReport {

        @Test
        @DisplayName("无记录 → 抛异常")
        void noRecordsShouldThrow() {
            when(reciteRecordPort.findBySessionId(1L, "s1")).thenReturn(List.of());

            assertThatThrownBy(() -> service.generateReport(1L, "s1"))
                    .isInstanceOf(ReportException.class)
                    .extracting("code").isEqualTo("400");
        }

        @Test
        @DisplayName("有记录 → LLM 生成 + 存 journal")
        void recordsShouldGenerateReport() {
            var record = ReciteRecordEntity.builder()
                    .id(1L).userId(1L).sessionId("s1").score(7).moduleKey("jvm")
                    .questionId("q1").userAnswer("答").build();
            when(reciteRecordPort.findBySessionId(1L, "s1")).thenReturn(List.of(record));
            when(reportPort.findRecentJournals(1L, 5)).thenReturn(List.of());
            when(llmPort.generateJournalReport(anyList(), anyList()))
                    .thenReturn("{\"overall\":\"不错\"}");

            var result = service.generateReport(1L, "s1");

            assertThat(result.getSummaryJson()).isEqualTo("{\"overall\":\"不错\"}");
            verify(reportPort).save(any(LearningJournal.class));
        }
    }

    // ---- helper ----

    private UserProgressEntity createProgress(int count, int mastery, String module) {
        return UserProgressEntity.builder()
                .id(1L).userId(1L).questionId("q1").moduleKey(module)
                .reciteCount(count).masteryScore(mastery).averageScore(7.0)
                .reviewInterval(3).easeFactor(2.5)
                .lastRecitedAt(LocalDateTime.now()).nextReviewAt(LocalDateTime.now().plusDays(3))
                .build();
    }
}
