package cn.bugstack.recite.domain.admin.service;

import cn.bugstack.recite.domain.admin.port.out.DashboardPort;
import cn.bugstack.recite.domain.admin.port.out.DashboardPort.Overview;
import cn.bugstack.recite.domain.admin.port.out.DashboardPort.TrendPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仪表盘聚合服务 — 薄层，委托 DashboardPort.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardPort dashboardPort;

    public Overview getOverview() {
        return dashboardPort.getOverview();
    }

    public List<TrendPoint> getTrends(int days) {
        return dashboardPort.getTrends(days);
    }
}
