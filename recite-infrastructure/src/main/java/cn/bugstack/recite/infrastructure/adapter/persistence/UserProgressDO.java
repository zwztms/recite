package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 user_progress 表.
 */
@Data
@TableName("user_progress")
public class UserProgressDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String questionId;
    private String moduleKey;
    private Integer masteryScore;
    private Integer reciteCount;
    private Double averageScore;
    private LocalDateTime lastRecitedAt;
    private LocalDateTime nextReviewAt;
    private Integer reviewInterval;
    private Double easeFactor;
}
