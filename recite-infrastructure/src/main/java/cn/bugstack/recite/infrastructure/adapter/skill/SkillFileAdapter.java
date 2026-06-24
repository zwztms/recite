package cn.bugstack.recite.infrastructure.adapter.skill;

import cn.bugstack.recite.domain.recite.service.SkillRegistry.SkillFileLoader;
import cn.bugstack.recite.types.skill.SkillDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * 扫描 classpath:skills/* /SKILL.md，解析 YAML frontmatter + Markdown body.
 */
@Slf4j
@Component
public class SkillFileAdapter implements SkillFileLoader {

    private static final String PATTERN = "classpath:skills/*/SKILL.md";
    private static final Pattern FM = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)", Pattern.DOTALL);

    @Override
    public List<SkillDefinition> loadAll() {
        List<SkillDefinition> result = new ArrayList<>();
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            for (Resource res : resolver.getResources(PATTERN)) {
                try {
                    String content = new BufferedReader(new InputStreamReader(
                            res.getInputStream(), StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                    SkillDefinition def = parse(content);
                    if (def != null) result.add(def);
                } catch (Exception e) {
                    log.warn("解析 SKILL.md 失败: {} — {}", res.getFilename(), e.getMessage());
                }
            }
            if (result.isEmpty()) {
                log.info("skills/ 目录为空，无 skill 注册");
            }
        } catch (Exception e) {
            log.warn("扫描 skills/ 失败: {}", e.getMessage());
        }
        return result;
    }

    SkillDefinition parse(String content) {
        if (content == null || content.isBlank()) return null;
        Matcher m = FM.matcher(content);
        if (!m.find()) {
            log.warn("SKILL.md 缺少 YAML frontmatter，跳过");
            return null;
        }
        String yaml = m.group(1);
        String body = m.group(2).trim();
        String name = extract(yaml, "name");
        if (name == null || name.isBlank()) {
            log.warn("name 缺失，跳过");
            return null;
        }
        String label = extract(yaml, "label");
        String desc = extract(yaml, "description");
        return new SkillDefinition(name.trim(),
                label != null ? label.trim() : name.trim(),
                desc != null ? desc.trim() : "",
                body);
    }

    private String extract(String yaml, String key) {
        Matcher m = Pattern.compile("^" + Pattern.quote(key) + ":\\s*(.*?)$", Pattern.MULTILINE).matcher(yaml);
        return m.find() ? m.group(1).trim() : null;
    }
}
