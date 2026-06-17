package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.knowledge.model.valueobj.EmbeddedQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * question_vectors 表 Mapper — 含自定义 pgvector 搜索.
 */
@Mapper
public interface QuestionVectorMapper extends BaseMapper<QuestionVectorDO> {

    /**
     * pgvector 余弦相似度搜索.
     * <p>HNSW 索引自动使用（m=48, ef_construction=200）.</p>
     */
    @Select("""
            SELECT qv.*, 1 - (embedding <=> CAST(#{queryVector} AS vector)) AS similarity
            FROM question_vectors qv
            WHERE module_key = ANY(#{moduleKeys})
              AND tags NOT LIKE '%status:OFFLINE%'
            ORDER BY embedding <=> CAST(#{queryVector} AS vector)
            LIMIT #{limit}
            """)
    List<QuestionVectorDO> searchByVector(@Param("queryVector") String queryVector,
                                          @Param("moduleKeys") String[] moduleKeys,
                                          @Param("limit") int limit);

    /** 按模块查询 */
    @Select("SELECT * FROM question_vectors WHERE module_key = #{moduleKey} LIMIT #{limit}")
    List<QuestionVectorDO> findByModule(@Param("moduleKey") String moduleKey,
                                        @Param("limit") int limit);

    /** 按模块统计题目数 */
    @Select("SELECT COUNT(*) FROM question_vectors WHERE module_key = #{moduleKey}")
    int countByModule(@Param("moduleKey") String moduleKey);
}
