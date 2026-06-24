package cn.bugstack.recite.domain.admin.port.out;

import java.util.List;

/**
 * 仪表盘数据聚合 SPI.
 * <p>
 * 跨 users / recite_records / user_progress 三表聚合，由 infra 层 JdbcTemplate 实现.
 */
public interface DashboardPort {

    Overview getOverview();

    List<TrendPoint> getTrends(int days);

    record Overview(int totalUsers, int totalSessions, double avgScore,
                    double masteryRate, List<ModuleStat> topModules) {}

    record ModuleStat(String moduleKey, String moduleName, int sessionCount) {}

    record TrendPoint(String date, int sessions, double avgScore, int activeUsers) {}
}
