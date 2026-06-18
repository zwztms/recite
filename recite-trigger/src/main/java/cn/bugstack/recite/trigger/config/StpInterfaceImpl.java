package cn.bugstack.recite.trigger.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.bugstack.recite.domain.auth.port.out.AdminUserPort;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色扩展 — JWT 模式下为 checkRole 提供角色来源.
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private AdminUserPort adminUserPort;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) return Collections.emptyList();
        try {
            long id = Long.parseLong(loginId.toString());
            if (adminUserPort.existsById(id)) {
                return List.of("ADMIN");
            }
        } catch (NumberFormatException ignored) {
        }
        return Collections.emptyList();
    }
}
