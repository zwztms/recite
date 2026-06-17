package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 映射 question_vectors 表，含 pgvector embedding 字段.
 */
@Data
@TableName("question_vectors")
public class QuestionVectorDO {

    @TableId
    private String id;
    private String content;
    private String question;
    private String moduleKey;
    private String category;
    private String tags;
    private Integer difficulty;
    /** pgvector 1024 维向量，MyBatis Plus 通过自定义 type handler 或原生 SQL 处理 */
    private float[] embedding;
}
