package cn.bugstack.recite.domain.recite.service;

import cn.bugstack.recite.domain.recite.port.out.SkillPort.ToolDefinition;
import cn.bugstack.recite.types.skill.SkillDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Skill 注册中心 — 领域服务.
 * 启动时通过 SkillFileLoader 加载全部 SKILL.md，缓存到 LinkedHashMap.
 * 参考 Ragent DefaultMCPToolRegistry 模式.
 */
@Slf4j
public class SkillRegistry {

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    public SkillRegistry(SkillFileLoader loader) {
        List<SkillDefinition> loaded = loader.loadAll();
        for (SkillDefinition s : loaded) {
            if (s.name() == null || s.name().isBlank()) {
                log.warn("跳过无效 skill: name 为空");
                continue;
            }
            if (s.body() == null || s.body().isBlank()) {
                log.warn("跳过无效 skill [{}]: body 为空", s.name());
                continue;
            }
            skills.put(s.name(), s);
            log.info("Skill 注册成功: name={}, label={}", s.name(), s.label());
        }
        log.info("SkillRegistry 初始化完成, 共注册 {} 个 skill", skills.size());
    }

    public List<ToolDefinition> buildToolDefinitions() {
        return skills.values().stream()
                .map(s -> ToolDefinition.of(s.name(), s.description()))
                .toList();
    }

    public SkillDefinition get(String name) {
        return skills.get(name);
    }

    public int size() {
        return skills.size();
    }

    /** 文件加载器 — 由 infra 层实现 */
    public interface SkillFileLoader {
        List<SkillDefinition> loadAll();
    }
}
