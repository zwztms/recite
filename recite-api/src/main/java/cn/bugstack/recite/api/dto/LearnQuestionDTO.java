package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 翻卡学习题目响应 — 含掌握状态.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnQuestionDTO {

    private String id;
    /** 题目 */
    private String question;
    /** 答案 */
    private String content;
    private String moduleKey;
    private String moduleName;
    private String category;
    private String tags;
    private int difficulty;
    /** 当前用户是否已掌握 (masteryScore >= 80) */
    private boolean mastered;
}
