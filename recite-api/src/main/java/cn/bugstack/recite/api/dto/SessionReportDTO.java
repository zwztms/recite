package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话报告响应.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionReportDTO {
    private Double totalScore;
    private Double averageScore;
    private Integer totalQuestions;
    private List<String> strengths;
    private List<String> weaknesses;
    private String advice;
}
