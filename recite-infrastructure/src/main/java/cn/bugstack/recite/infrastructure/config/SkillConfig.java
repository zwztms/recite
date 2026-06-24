package cn.bugstack.recite.infrastructure.config;

import cn.bugstack.recite.domain.recite.service.SkillRegistry;
import cn.bugstack.recite.infrastructure.adapter.skill.SkillFileAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SkillConfig {

    @Bean
    public SkillRegistry skillRegistry(SkillFileAdapter loader) {
        return new SkillRegistry(loader);
    }
}
