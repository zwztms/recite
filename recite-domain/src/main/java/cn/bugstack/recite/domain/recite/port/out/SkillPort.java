package cn.bugstack.recite.domain.recite.port.out;

import cn.bugstack.recite.types.skill.SkillResultVO;

import java.util.List;
import java.util.Map;

/**
 * Skill 发现与执行 SPI — 定义在 domain，实现在 infra.
 */
public interface SkillPort {

    /** 获取所有 skill 的 tool 定义（传给 DeepSeek API 的 tools 参数） */
    List<ToolDefinition> listToolDefinitions();

    /** 执行指定 skill（子 LLM 调用） */
    SkillResultVO execute(String skillName, Map<String, Object> params);

    /** DeepSeek tool 定义 */
    record ToolDefinition(String name, String description, Map<String, Object> parameters) {
        public static ToolDefinition of(String name, String description) {
            return new ToolDefinition(name, description,
                    Map.of("type", "object", "properties", Map.of(), "required", List.of()));
        }
    }
}
