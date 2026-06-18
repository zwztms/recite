package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 achievement_log 表.
 */
@Data
@TableName("achievement_log")
public class AchievementLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String badgeKey;
    private LocalDateTime earnedAt;
}
