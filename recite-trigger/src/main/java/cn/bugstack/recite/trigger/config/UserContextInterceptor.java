package cn.bugstack.recite.trigger.config;

import cn.bugstack.recite.domain.auth.model.entity.UserEntity;
import cn.bugstack.recite.domain.auth.port.out.UserPort;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器 — 从 Header 取 token → 解析 → 查库 → 注入 TTL.
 */
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserPort userPort;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            return true; // 放行, 由 Sa-Token 注解拦截
        }

        // Sa-Token 解析 token → loginId
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) return true;

            Long userId = Long.valueOf(loginId.toString());
            UserEntity user = userPort.findById(userId);
            if (user != null) {
                UserContext.set(userId, user.getRole());
            }
        } catch (Exception ignored) {
            // token 无效，放行由业务层决定
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
