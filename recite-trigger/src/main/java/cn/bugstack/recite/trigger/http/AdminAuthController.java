package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.auth.exception.AuthException;
import cn.bugstack.recite.domain.auth.model.entity.AdminUserEntity;
import cn.bugstack.recite.domain.auth.port.out.AdminUserPort;
import cn.bugstack.recite.types.common.PasswordUtils;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员认证控制器.
 */
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserPort adminUserPort;

    @PostMapping("/login")
    public Response<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new AuthException("用户名和密码不能为空");
        }

        AdminUserEntity admin = adminUserPort.findByUsername(username);
        if (admin == null || !admin.getPasswordHash().equals(PasswordUtils.hash(password))) {
            throw new AuthException("用户名或密码错误");
        }

        StpUtil.login(admin.getId());
        StpUtil.getSession().set("role", "ADMIN");

        return Response.ok(Map.of("token", StpUtil.getTokenValue(), "username", admin.getUsername()));
    }
}
