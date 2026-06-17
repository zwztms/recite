package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 映射 user_streak 表.
 */
@Data
@TableName("user_streak")
public class UserStreakDO {

    @TableId
    private Long userId;
    private Integer currentStreak;
    private LocalDate lastActiveDate;
    private Integer longestStreak;
}
