package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 learning_journal 表.
 */
@Data
@TableName("learning_journal")
public class LearningJournalDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionId;
    /** JSONB — LLM 返回的结构化报告 */
    private String summaryJson;
    private LocalDateTime createdAt;
}
