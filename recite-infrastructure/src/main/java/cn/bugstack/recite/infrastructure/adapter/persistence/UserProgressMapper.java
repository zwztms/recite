package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * user_progress 表 Mapper.
 */
@Mapper
public interface UserProgressMapper extends BaseMapper<UserProgressDO> {

    @Select("SELECT * FROM user_progress WHERE user_id = #{userId} AND next_review_at <= NOW() ORDER BY next_review_at ASC LIMIT #{limit}")
    List<UserProgressDO> findDueQuestions(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_progress WHERE user_id = #{userId} AND mastery_score >= 80")
    int countMastered(@Param("userId") Long userId);
}
