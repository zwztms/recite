package cn.bugstack.recite.domain.recite.model.valueobj;

import java.util.List;

/**
 * 会话报告值对象.
 */
public record SessionReportVO(
        Double totalScore,
        Double averageScore,
        Integer totalQuestions,
        /** 优势模块名 */
        List<String> strengths,
        /** 薄弱模块名 */
        List<String> weaknesses,
        /** LLM 综合评语 */
        String advice) {
}
