package cn.bugstack.recite.domain.report.service;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报告领域服务 — 仪表盘聚合 + 报告生成编排.
 */
@Slf4j
@Service
public class ReportService {

    private final ReciteRecordPort reciteRecordPort;
    private final ProgressPort progressPort;
    private final StreakPort streakPort;
    private final ReportPort reportPort;
    private final LlmPort llmPort;

    public ReportService(ReciteRecordPort reciteRecordPort,
                         ProgressPort progressPort,
                         StreakPort streakPort,
                         ReportPort reportPort,
                         LlmPort llmPort) {
        this.reciteRecordPort = reciteRecordPort;
        this.progressPort = progressPort;
        this.streakPort = streakPort;
        this.reportPort = reportPort;
        this.llmPort = llmPort;
    }

    /** MQ 消费者调用：统计 + LLM 评语 → 存入 learning_journal */
    public LearningJournal generateReport(Long userId, String sessionId) {
        // ① 查本轮记录
        List<ReciteRecordEntity> records = reciteRecordPort.findBySessionId(userId, sessionId);
        if (records == null || records.isEmpty()) {
            throw new ReportException("400", "本轮无背诵记录");
        }

        // ② 查历史档案（注入 LLM 上下文）
        List<LearningJournal> recentJournals = reportPort.findRecentJournals(userId, 5);
        List<String> recentSummaries = recentJournals.stream()
                .map(LearningJournal::getSummaryJson)
                .filter(Objects::nonNull)
                .toList();

        // ③④ LLM 生成评语（内含 Java 统计）
        String summaryJson = llmPort.generateJournalReport(records, recentSummaries);

        // ⑤ 存入 learning_journal
        LearningJournal journal = LearningJournal.builder()
                .userId(userId)
                .sessionId(sessionId)
                .summaryJson(summaryJson)
                .createdAt(LocalDateTime.now())
                .build();
        reportPort.save(journal);
        log.info("报告已生成: userId={}, sessionId={}", userId, sessionId);
        return journal;
    }

    /** 仪表盘聚合：4 卡 + 模块条 + 趋势 + 薄弱标签 + 最新建议 */
    public DashboardVO aggregateDashboard(Long userId) {
        // ① 4 栏统计卡
        List<UserProgressEntity> progressList = progressPort.findByUserId(userId);
        int totalQuestions = progressList.stream().mapToInt(UserProgressEntity::getReciteCount).sum();
        double avgMastery = progressList.stream()
                .mapToInt(UserProgressEntity::getMasteryScore).average().orElse(0);

        Optional<UserStreakEntity> streakOpt = streakPort.findByUserId(userId);
        int streakDays = streakOpt.map(UserStreakEntity::getCurrentStreak).orElse(0);

        int reportCount = reportPort.countByUserId(userId);

        StatsCard card = new StatsCard(totalQuestions, avgMastery, streakDays, reportCount);

        // ② 模块横向条
        Map<String, Double> moduleBars = progressList.stream()
                .collect(Collectors.groupingBy(UserProgressEntity::getModuleKey,
                        Collectors.averagingInt(UserProgressEntity::getMasteryScore)));

        // ③ 趋势数据（最近 7 次）
        List<LearningJournal> recentJournals = reportPort.findRecentJournals(userId, 7);
        List<TrendPoint> trend = recentJournals.stream()
                .map(j -> new TrendPoint(j.getCreatedAt(), j.getSummaryJson()))
                .toList();

        // ④ 薄弱标签（从最近 5 次汇总去重）
        List<String> weakTags = recentJournals.stream().limit(5)
                .map(LearningJournal::getSummaryJson)
                .filter(Objects::nonNull)
                .flatMap(json -> {
                    try {
                        var gson = new com.google.gson.Gson();
                        var map = gson.<Map<String, Object>>fromJson(json,
                                new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
                        @SuppressWarnings("unchecked")
                        List<String> tags = (List<String>) map.getOrDefault("weakTags", List.of());
                        return tags.stream();
                    } catch (Exception e) { return java.util.stream.Stream.empty(); }
                }).distinct().toList();

        // ⑤ 最新 AI 建议
        String latestAdvice = recentJournals.stream().findFirst()
                .map(j -> {
                    try {
                        var map = new com.google.gson.Gson().<Map<String, Object>>fromJson(j.getSummaryJson(),
                                new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
                        return (String) map.getOrDefault("advice", "");
                    } catch (Exception e) { return ""; }
                }).orElse("");

        return new DashboardVO(card, moduleBars, trend, weakTags, latestAdvice);
    }

    // ---- 仪表盘值对象 ----

    public record StatsCard(int totalQuestions, double avgMastery, int streakDays, int reportCount) {}
    public record TrendPoint(LocalDateTime date, String summaryJson) {}
    public record DashboardVO(StatsCard statsCard, Map<String, Double> moduleBars,
                               List<TrendPoint> trend, List<String> weakTags, String latestAdvice) {}
}
