package cn.bugstack.recite.types.skill;

/**
 * 从 SKILL.md 解析后的 skill 定义.
 * YAML frontmatter 提取 name/label/description，body 是完整 system prompt.
 */
public record SkillDefinition(
        String name,          // 唯一标识，DeepSeek tool function name
        String label,         // 前端展示名
        String description,   // tool description，决定 LLM 何时调用
        String body) {}       // Markdown body，子 LLM 的 system prompt
