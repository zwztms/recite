package cn.bugstack.recite.trigger.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token JWT 配置.
 */
@Configuration
public class SaTokenConfig {

    /** JWT 模式 — token 为自包含 JWT，不依赖 Redis */
    @Bean
    public StpLogic stpLogic() {
        return new StpLogicJwtForSimple();
    }
}
