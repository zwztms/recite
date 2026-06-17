package cn.bugstack.recite.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    private Long id;
    /** 手机号，唯一 */
    private String phone;
    /** SHA-256 哈希 */
    private String passwordHash;
    /** 昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatar;
    /** USER / ADMIN */
    private String role;
    /** ACTIVE / DISABLED */
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
