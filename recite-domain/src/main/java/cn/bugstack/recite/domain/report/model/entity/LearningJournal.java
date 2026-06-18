package cn.bugstack.recite.domain.report.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习档案实体 — 映射 learning_journal 表.
 *
 * <p>summaryJson 存储 LLM 返回的结构化报告 JSON:
 * { totalScore, averageScore, totalQuestions,
 *   summary, strengths:[], weaknesses:[],
 *   advice, trendComment, weakTags:[],
 *   moduleScores: [{moduleKey, moduleName, avgScore, count}] }</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningJournal {

    private Long id;
    private Long userId;
    private String sessionId;
    /** JSONB 字段，LLM 返回的结构化报告 */
    private String summaryJson;
    private LocalDateTime createdAt;
}
