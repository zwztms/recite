package cn.bugstack.recite.domain.recite.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 背诵记录实体 — PG recite_records 表.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReciteRecordEntity {

    private Long id;
    private Long userId;
    private String sessionId;
    /** CATEGORY / RANDOM / REVIEW */
    private String mode;
    private String moduleKey;
    private String questionId;
    private String userAnswer;
    /** 1-10，REVIEW 模式为 null */
    private Integer score;
    /** correctPoints 拼接 */
    private String feedback;
    /** LLM 生成的追问 */
    private String followUpQuestion;
    /** 用户追问回答 */
    private String followUpAnswer;
    /** 追问反馈 */
    private String followUpFeedback;
    /** 追问层数 */
    private int followUpDepth;
    /** 追问链父记录 ID */
    private Long parentRecordId;
    /** 单题答题耗时（秒） */
    private Integer responseTimeSeconds;
    private LocalDateTime createdAt;
}
