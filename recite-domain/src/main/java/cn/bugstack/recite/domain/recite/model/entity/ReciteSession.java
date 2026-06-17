package cn.bugstack.recite.domain.recite.model.entity;

import cn.bugstack.recite.types.enums.ReciteMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 背诵会话 — Redis JSON 存储对象，TTL 2h.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReciteSession {

    /** UUID */
    private String sessionId;
    /** 所属用户 */
    private Long userId;
    /** CATEGORY / RANDOM / REVIEW */
    private ReciteMode mode;
    /** 模块范围 */
    private List<String> moduleKeys;
    /** 当前题目 ID */
    private String currentQuestionId;
    /** 本次全部题目 ID 列表（startRecite 时拉取） */
    private List<String> questionIds;
    /** 当前题目序号，0-based */
    private int currentIndex;
    /** 总题数 */
    private int totalQuestions;
    /** ACTIVE / FINISHED */
    private String status;
    /** 当前追问层数 0-3 */
    private int followUpDepth;
    private LocalDateTime createdAt;
}
