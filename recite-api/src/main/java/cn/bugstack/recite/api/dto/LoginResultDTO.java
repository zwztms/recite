package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册成功响应.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDTO {
    private String token;
    private String role;
    private String nickname;
}
