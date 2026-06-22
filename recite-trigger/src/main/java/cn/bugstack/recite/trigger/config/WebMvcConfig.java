package cn.bugstack.recite.trigger.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 Sa-Token 鉴权拦截器 + UserContextInterceptor.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Sa-Token 鉴权：需登录的接口
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/recite/**", "/home/**", "/learn/**");

        // Sa-Token 鉴权：知识库管理需管理员
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkRole("ADMIN")))
                .addPathPatterns("/admin/knowledge/**");

        // Sa-Token 鉴权：运维监控需管理员
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkRole("ADMIN")))
                .addPathPatterns("/admin/monitor/**");

        // 用户上下文注入（需在鉴权之后，此时 token 已验证）
        registry.addInterceptor(userContextInterceptor)
                .excludePathPatterns(
                        "/auth/**",
                        "/admin/auth/**",
                        "/admin/monitor/**",
                        "/doc.html",
                        "/v3/**",
                        "/error"
                );
    }
}
