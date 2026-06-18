package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private StatsCard statsCard;
    private Map<String, Double> moduleBars;
    private List<TrendPoint> trend;
    private List<String> weakTags;
    private String latestAdvice;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsCard {
        private int totalQuestions;
        private double avgMastery;
        private int streakDays;
        private int reportCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private double avgScore;
    }
}
