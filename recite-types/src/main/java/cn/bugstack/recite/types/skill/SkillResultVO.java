package cn.bugstack.recite.types.skill;

import java.util.Map;

/**
 * 单次 skill 执行结果，通过 SSE event:skill 推给前端.
 */
public record SkillResultVO(
        String name,
        String label,
        String resultJson,                        // 子 LLM 返回的原始 JSON
        Map<String, Object> structuredData) {     // 解析后的结构化数据

    public static SkillResultVO error(String name, String label, String message) {
        return new SkillResultVO(name, label,
                "{\"error\":\"" + message + "\"}", Map.of("error", message));
    }
}
