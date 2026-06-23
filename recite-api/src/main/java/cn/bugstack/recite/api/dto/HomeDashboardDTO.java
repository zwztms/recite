package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 个人主页仪表盘聚合 DTO — 一次返回主页全部 8 区域数据.
 *
 * <p>对应 GET /home/dashboard，由 HomeService 跨子域聚合.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeDashboardDTO {

    private UserInfo user;
    private Stats stats;
    private List<ModuleMastery> moduleMastery;
    private List<TrendBar> trend;
    private List<BadgeItem> badges;
    private List<String> weakTags;
    private String advice;
    private List<RecentRecite> recentRecites;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String nickname;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private int streakDays;
        private int totalRecites;
        private int masteredCount;
        /** 0-100 */
        private int totalProgress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleMastery {
        private String moduleKey;
        private String moduleName;
        /** 已掌握题数 */
        private int mastered;
        /** 该模块总题数 */
        private int total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendBar {
        /** "一"~"日" */
        private String dayLabel;
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeItem {
        private String key;
        private String name;
        private String icon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRecite {
        private String sessionId;
        /** "今天 14:30" / "昨天 21:40" / "6月20日" */
        private String dateLabel;
        private String moduleKey;
        private String moduleName;
        private int questionCount;
        private double avgScore;
    }
}
