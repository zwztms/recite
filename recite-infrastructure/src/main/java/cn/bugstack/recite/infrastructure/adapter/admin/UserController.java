package cn.bugstack.recite.infrastructure.adapter.admin;

import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.infrastructure.adapter.persistence.*;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理控制器.
 * <p>
 * 放在 infrastructure 模块是因为：直接使用 Mapper 做跨表 CRUD，与 AdminMonitorController 同级.
 */
@RestController
@RequestMapping("/admin")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final ReciteRecordMapper reciteRecordMapper;

    /** 分页用户列表 */
    @GetMapping("/users")
    public Response<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        Page<UserDO> p = new Page<>(page, size);
        LambdaQueryWrapper<UserDO> qw = new LambdaQueryWrapper<UserDO>()
                .ne(UserDO::getStatus, "DELETED");
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(UserDO::getPhone, keyword));
        }
        qw.orderByDesc(UserDO::getCreatedAt);
        userMapper.selectPage(p, qw);

        List<UserVO> vos = p.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setPhone(maskPhone(u.getPhone()));
            vo.setRole(isAdmin(u.getId()) ? "admin" : "user");
            vo.setCreatedAt(u.getCreatedAt());
            Long reciteCount = reciteRecordMapper.selectCount(
                    new LambdaQueryWrapper<ReciteRecordDO>().eq(ReciteRecordDO::getUserId, u.getId()));
            vo.setReciteCount(reciteCount.intValue());
            return vo;
        }).toList();

        PageResult<UserVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(p.getTotal());
        result.setPage(page);
        result.setSize(size);
        return Response.ok(result);
    }

    /** 修改用户角色 */
    @PutMapping("/users/{id}")
    public Response<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if ("admin".equals(role)) {
            AdminUserDO au = new AdminUserDO();
            au.setId(id);
            adminUserMapper.insert(au);
        } else {
            adminUserMapper.delete(new LambdaQueryWrapper<AdminUserDO>().eq(AdminUserDO::getId, id));
        }
        return Response.ok();
    }

    /** 删除用户（软删除） */
    @DeleteMapping("/users/{id}")
    public Response<?> deleteUser(@PathVariable Long id) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (id.equals(currentUserId)) {
            return Response.fail("403", "不能删除自己");
        }
        UserDO u = new UserDO();
        u.setId(id);
        u.setStatus("DELETED");
        userMapper.updateById(u);
        return Response.ok();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private boolean isAdmin(Long userId) {
        return adminUserMapper.selectCount(
                new LambdaQueryWrapper<AdminUserDO>().eq(AdminUserDO::getId, userId)) > 0;
    }

    // ---- VO ----

    @Data
    public static class UserVO {
        private Long id;
        private String phone;
        private String role;
        private int reciteCount;
        private LocalDateTime createdAt;
    }

    @Data
    public static class PageResult<T> {
        private List<T> records;
        private long total;
        private int page;
        private int size;
    }
}
