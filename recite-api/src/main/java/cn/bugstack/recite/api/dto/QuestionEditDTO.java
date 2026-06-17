package cn.bugstack.recite.api.dto;

import lombok.Data;

/**
 * 编辑题目请求.
 */
@Data
public class QuestionEditDTO {
    private String question;
    private String content;
    private String moduleKey;
}
