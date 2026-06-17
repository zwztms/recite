package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * knowledge_modules 表 Mapper.
 */
@Mapper
public interface KnowledgeModuleMapper extends BaseMapper<KnowledgeModuleDO> {
}
