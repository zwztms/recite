package cn.bugstack.recite.api.dto;

import lombok.Data;

/**
 * 题目管理列表响应（管理后台用）.
 */
@Data
public class QuestionManageDTO {
    private String id;
    private String question;
    private String content;
    private String moduleKey;
    private String category;
    private String tags;
    private Integer difficulty;
    /** ONLINE / OFFLINE，从 tags 中解析 */
    private String status;
}
