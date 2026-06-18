package cn.bugstack.recite.trigger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / Swagger 配置 — 自动扫描所有 Controller 端点，访问 /doc.html.
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI reciteOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("八股文背诵助手 API")
                        .version("2.0")
                        .description("recite-v2 DDD 六模块 · 五子域"));
    }
}
