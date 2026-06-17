package cn.bugstack.recite.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员实体.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserEntity {

    private Long id;
    /** 登录名 */
    private String username;
    /** SHA-256 哈希 */
    private String passwordHash;
    private String nickname;
    /** ACTIVE / DISABLED */
    private String status;
    private LocalDateTime createdAt;
}
