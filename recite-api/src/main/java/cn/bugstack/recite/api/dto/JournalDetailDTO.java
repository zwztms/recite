package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习档案详情 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalDetailDTO {

    private Long id;
    private String sessionId;
    private Double totalScore;
    private Double averageScore;
    private Integer totalQuestions;
    private List<String> strengths;
    private List<String> weaknesses;
    private String advice;
    private String trendComment;
    private List<String> weakTags;
    private List<ModuleScore> moduleScores;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleScore {
        private String moduleKey;
        private String moduleName;
        private double avgScore;
        private int count;
    }
}
