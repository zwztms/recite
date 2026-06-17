package cn.bugstack.recite.domain.knowledge.port.out;

import cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity;

import java.util.List;
import java.util.Optional;

/**
 * 模块管理 SPI — infra 层实现.
 */
public interface ModulePort {

    /** 全部模块，按 sortOrder 排序 */
    List<KnowledgeModuleEntity> listAll();

    /** 仅 ONLINE 状态的模块 */
    List<KnowledgeModuleEntity> listOnline();

    /** 按 moduleKey 查询 */
    Optional<KnowledgeModuleEntity> findByKey(String moduleKey);

    /** 新增模块 */
    void save(KnowledgeModuleEntity module);

    /** 上下线 */
    void updateStatus(String moduleKey, String status);

    /** 题目计数增减（delta 可正可负） */
    void updateQuestionCount(String moduleKey, int delta);

    /** 删除模块 */
    void delete(String moduleKey);
}
