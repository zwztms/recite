package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.LoginRequestDTO;
import cn.bugstack.recite.api.dto.LoginResultDTO;
import cn.bugstack.recite.api.dto.RegisterRequestDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 用户认证 REST 接口.
 */
@RequestMapping("/auth")
public interface IAuthService {

    @PostMapping("/register")
    Response<LoginResultDTO> register(@RequestBody RegisterRequestDTO request);

    @PostMapping("/login")
    Response<LoginResultDTO> login(@RequestBody LoginRequestDTO request);
}
