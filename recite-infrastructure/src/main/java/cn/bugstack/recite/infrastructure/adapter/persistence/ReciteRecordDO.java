package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 recite_records 表.
 */
@Data
@TableName("recite_records")
public class ReciteRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionId;
    private String mode;
    private String moduleKey;
    private String questionId;
    private String userAnswer;
    private Integer score;
    private String feedback;
    private String followUpQuestion;
    private String followUpAnswer;
    private String followUpFeedback;
    private Integer followUpDepth;
    private Long parentRecordId;
    private Integer responseTimeSeconds;
    private LocalDateTime createdAt;
}
