package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {
    private String phone;
    private String password;
    private String nickname;
}
