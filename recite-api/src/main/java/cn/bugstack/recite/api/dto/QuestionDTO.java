package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题目搜索响应（背诵页用）.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private String id;
    private String question;
    private String content;
    private String moduleKey;
    private String category;
    private String tags;
    private Integer difficulty;
    private Double similarityScore;
}
