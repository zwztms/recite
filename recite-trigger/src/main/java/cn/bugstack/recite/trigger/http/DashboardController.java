package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.admin.port.out.DashboardPort.Overview;
import cn.bugstack.recite.domain.admin.port.out.DashboardPort.TrendPoint;
import cn.bugstack.recite.domain.admin.service.DashboardService;
import cn.dev33.satoken.annotation.SaCheckRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表盘 API — 管理员 KPI 概览 + 背诵趋势.
 */
@RestController
@RequestMapping("/admin/dashboard")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Response<Overview> overview() {
        return Response.ok(dashboardService.getOverview());
    }

    @GetMapping("/trends")
    public Response<List<TrendPoint>> trends(@RequestParam(defaultValue = "7") int days) {
        return Response.ok(dashboardService.getTrends(days));
    }
}
