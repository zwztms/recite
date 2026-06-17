package cn.bugstack.recite.domain.knowledge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题目实体.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEntity {

    /** UUID */
    private String id;
    /** 题目标题，最长 2000 */
    private String question;
    /** 答案正文 */
    private String content;
    /** 所属模块 key */
    private String moduleKey;
    /** 分类标签 */
    private String category;
    /** 标签字符串（空格分隔） */
    private String tags;
    /** 难度 1-5 */
    private Integer difficulty;
    /** 参考答案文本（导入原始答案字段，content 同时也是答案） */
    private String referenceAnswer;
    /** 1024 维向量 */
    private float[] embedding;
}
