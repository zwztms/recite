package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 模块管理适配器 — MyBatis Plus 实现.
 */
@Service
@RequiredArgsConstructor
public class ModuleManagementAdapter implements ModulePort {

    private final KnowledgeModuleMapper mapper;

    @Override
    public List<KnowledgeModuleEntity> listAll() {
        return mapper.selectList(
                new LambdaQueryWrapper<KnowledgeModuleDO>()
                        .orderByAsc(KnowledgeModuleDO::getSortOrder))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<KnowledgeModuleEntity> listOnline() {
        return mapper.selectList(
                new LambdaQueryWrapper<KnowledgeModuleDO>()
                        .eq(KnowledgeModuleDO::getStatus, "ONLINE")
                        .orderByAsc(KnowledgeModuleDO::getSortOrder))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<KnowledgeModuleEntity> findByKey(String moduleKey) {
        KnowledgeModuleDO d = mapper.selectOne(
                new LambdaQueryWrapper<KnowledgeModuleDO>()
                        .eq(KnowledgeModuleDO::getModuleKey, moduleKey));
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public void save(KnowledgeModuleEntity module) {
        KnowledgeModuleDO d = toDO(module);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        mapper.insert(d);
        module.setId(d.getId());
    }

    @Override
    public void updateStatus(String moduleKey, String status) {
        mapper.update(null, new LambdaUpdateWrapper<KnowledgeModuleDO>()
                .eq(KnowledgeModuleDO::getModuleKey, moduleKey)
                .set(KnowledgeModuleDO::getStatus, status)
                .set(KnowledgeModuleDO::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void updateQuestionCount(String moduleKey, int delta) {
        KnowledgeModuleDO d = mapper.selectOne(
                new LambdaQueryWrapper<KnowledgeModuleDO>()
                        .eq(KnowledgeModuleDO::getModuleKey, moduleKey));
        if (d != null) {
            int newCount = Math.max(0, (d.getQuestionCount() != null ? d.getQuestionCount() : 0) + delta);
            mapper.update(null, new LambdaUpdateWrapper<KnowledgeModuleDO>()
                    .eq(KnowledgeModuleDO::getModuleKey, moduleKey)
                    .set(KnowledgeModuleDO::getQuestionCount, newCount)
                    .set(KnowledgeModuleDO::getUpdatedAt, LocalDateTime.now()));
        }
    }

    @Override
    public void delete(String moduleKey) {
        mapper.delete(new LambdaQueryWrapper<KnowledgeModuleDO>()
                .eq(KnowledgeModuleDO::getModuleKey, moduleKey));
    }

    // ---- 转换 ----

    private KnowledgeModuleEntity toEntity(KnowledgeModuleDO d) {
        return KnowledgeModuleEntity.builder()
                .id(d.getId()).moduleKey(d.getModuleKey()).moduleName(d.getModuleName())
                .description(d.getDescription()).status(d.getStatus())
                .sortOrder(d.getSortOrder()).questionCount(d.getQuestionCount())
                .createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }

    private KnowledgeModuleDO toDO(KnowledgeModuleEntity e) {
        KnowledgeModuleDO d = new KnowledgeModuleDO();
        d.setId(e.getId()); d.setModuleKey(e.getModuleKey()); d.setModuleName(e.getModuleName());
        d.setDescription(e.getDescription()); d.setStatus(e.getStatus() != null ? e.getStatus() : "ONLINE");
        d.setSortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0);
        d.setQuestionCount(e.getQuestionCount() != null ? e.getQuestionCount() : 0);
        d.setCreatedAt(e.getCreatedAt()); d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}
