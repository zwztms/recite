package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.admin.port.out.DashboardPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仪表盘数据适配器 — JdbcTemplate 跨表聚合查询.
 * <p>
 * 聚合 users / recite_records / user_progress 三表，计算 KPI 指标和趋势数据.
 */
@Service
@RequiredArgsConstructor
public class DashboardAdapter implements DashboardPort {

    private final JdbcTemplate jdbc;

    @Override
    public Overview getOverview() {
        int totalUsers = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE deleted = false", Integer.class);

        int totalSessions = jdbc.queryForObject(
                "SELECT count(*) FROM recite_records", Integer.class);

        Double avgScore = jdbc.queryForObject(
                "SELECT COALESCE(AVG(ai_score), 0) FROM (SELECT ai_score FROM recite_records ORDER BY created_at DESC LIMIT 100) t",
                Double.class);
        if (avgScore == null) avgScore = 0.0;

        Double masteryRate = jdbc.queryForObject(
                "SELECT COALESCE(100.0 * SUM(CASE WHEN mastery_score >= 80 THEN 1 ELSE 0 END) / NULLIF(count(*),0), 0) FROM user_progress",
                Double.class);
        if (masteryRate == null) masteryRate = 0.0;

        List<ModuleStat> topModules = jdbc.query(
                "SELECT module_key, count(*) as cnt FROM recite_records GROUP BY module_key ORDER BY cnt DESC LIMIT 5",
                (rs, i) -> new ModuleStat(rs.getString("module_key"), rs.getString("module_key"), rs.getInt("cnt")));

        return new Overview(totalUsers, totalSessions,
                Math.round(avgScore * 10.0) / 10.0,
                Math.round(masteryRate * 10.0) / 10.0, topModules);
    }

    @Override
    public List<TrendPoint> getTrends(int days) {
        return jdbc.query(
                "SELECT to_char(created_at, 'MM-DD') as date, count(*) as sessions, " +
                        "COALESCE(AVG(ai_score), 0) as avg_score, COUNT(DISTINCT user_id) as active_users " +
                        "FROM recite_records WHERE created_at > now() - (? || ' days')::interval " +
                        "GROUP BY to_char(created_at, 'MM-DD') ORDER BY date",
                (rs, i) -> new TrendPoint(
                        rs.getString("date"), rs.getInt("sessions"),
                        Math.round(rs.getDouble("avg_score") * 10.0) / 10.0,
                        rs.getInt("active_users")),
                days);
    }
}
