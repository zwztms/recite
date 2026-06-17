package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IAuthService;
import cn.bugstack.recite.api.dto.LoginRequestDTO;
import cn.bugstack.recite.api.dto.LoginResultDTO;
import cn.bugstack.recite.api.dto.RegisterRequestDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.auth.exception.AuthException;
import cn.bugstack.recite.domain.auth.model.entity.UserEntity;
import cn.bugstack.recite.domain.auth.port.out.UserPort;
import cn.bugstack.recite.types.common.PasswordUtils;
import cn.bugstack.recite.types.enums.ResponseCode;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户认证控制器.
 */
@RestController
@RequiredArgsConstructor
public class AuthController implements IAuthService {

    private final UserPort userPort;

    @Override
    public Response<LoginResultDTO> register(RegisterRequestDTO request) {
        if (!StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getPassword())) {
            throw new AuthException(ResponseCode.BAD_REQUEST.getCode(), "手机号和密码不能为空");
        }

        UserEntity exist = userPort.findByPhone(request.getPhone());
        if (exist != null) {
            throw new AuthException("手机号已注册");
        }

        String hash = PasswordUtils.hash(request.getPassword());
        String nickname = StringUtils.hasText(request.getNickname())
                ? request.getNickname()
                : "用户" + System.currentTimeMillis() % 10000;

        UserEntity user = UserEntity.builder()
                .phone(request.getPhone()).passwordHash(hash).nickname(nickname)
                .role("USER").status("ACTIVE").createdAt(LocalDateTime.now()).build();
        userPort.save(user);

        StpUtil.login(user.getId());
        LoginResultDTO result = new LoginResultDTO(StpUtil.getTokenValue(), "USER", nickname);
        return Response.ok(result);
    }

    @Override
    public Response<LoginResultDTO> login(LoginRequestDTO request) {
        if (!StringUtils.hasText(request.getAccount()) || !StringUtils.hasText(request.getPassword())) {
            throw new AuthException(ResponseCode.BAD_REQUEST.getCode(), "账号和密码不能为空");
        }

        String account = request.getAccount();
        UserEntity user;
        if (account.matches("^1\\d{10}$")) {
            user = userPort.findByPhone(account);
        } else {
            // 按昵称查 — 简化实现，UserPort 暂不支持 findByName，直接查 phone
            user = userPort.findByPhone(account);
        }

        if (user == null) {
            throw new AuthException("账号或密码错误");
        }

        String hash = PasswordUtils.hash(request.getPassword());
        if (!hash.equals(user.getPasswordHash())) {
            throw new AuthException("账号或密码错误");
        }

        StpUtil.login(user.getId());
        String role = user.getRole() != null ? user.getRole() : "USER";
        LoginResultDTO result = new LoginResultDTO(StpUtil.getTokenValue(), role, user.getNickname());
        return Response.ok(result);
    }
}
