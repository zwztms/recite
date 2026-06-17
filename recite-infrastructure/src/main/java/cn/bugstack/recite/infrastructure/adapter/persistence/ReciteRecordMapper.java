package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * recite_records 表 Mapper — 含统计查询.
 */
@Mapper
public interface ReciteRecordMapper extends BaseMapper<ReciteRecordDO> {

    @Select("SELECT COUNT(*) FROM recite_records WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT COALESCE(AVG(score), 0) FROM recite_records WHERE user_id = #{userId} AND score IS NOT NULL")
    double avgScoreByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM recite_records WHERE user_id = #{userId} AND module_key = #{moduleKey}")
    int countByModule(@Param("userId") Long userId, @Param("moduleKey") String moduleKey);

    @Select("SELECT COUNT(*) FROM recite_records WHERE user_id = #{userId} AND score = 10")
    int countPerfectScores(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT session_id) FROM recite_records WHERE user_id = #{userId}")
    int countSessionsByUserId(@Param("userId") Long userId);
}
