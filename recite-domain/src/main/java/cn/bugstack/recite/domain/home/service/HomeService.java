package cn.bugstack.recite.domain.home.service;

import cn.bugstack.recite.domain.achievement.port.out.AchievementPort;
import cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.domain.report.model.entity.LearningJournal;
import cn.bugstack.recite.domain.report.port.out.ReportPort;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个人主页领域服务 — 跨子域聚合编排.
 *
 * <p>一次 build() 返回主页全部 8 区域数据，注入 7 个已有 Port，
 * 不新增表、不新增 SPI.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final ReciteRecordPort reciteRecordPort;
    private final ProgressPort progressPort;
    private final StreakPort streakPort;
    private final AchievementPort achievementPort;
    private final ReportPort reportPort;
    private final QuestionPort questionPort;
    private final ModulePort modulePort;

    private static final Gson gson = new Gson();

    /** 聚合全部主页数据 */
    public HomeDashboardVO build(Long userId) {
        return new HomeDashboardVO(
                buildUserInfo(userId),
                buildStats(userId),
                buildModuleMastery(userId),
                buildTrend(userId),
                buildBadges(userId),
                buildWeakTags(userId),
                buildAdvice(userId),
                buildRecentRecites(userId)
        );
    }

    // ======== 用户信息 ========

    private UserInfo buildUserInfo(Long userId) {
        return new UserInfo("用户" + userId);
    }

    // ======== 统计卡片 ========

    private Stats buildStats(Long userId) {
        int streakDays = streakPort.findByUserId(userId)
                .map(UserStreakEntity::getCurrentStreak).orElse(0);
        int totalRecites = reciteRecordPort.countByUserId(userId);
        int masteredCount = progressPort.countMastered(userId);
        int totalQuestions = modulePort.listAll().stream()
                .mapToInt(m -> questionPort.countByModule(m.getModuleKey())).sum();
        int totalProgress = totalQuestions > 0 ? masteredCount * 100 / totalQuestions : 0;
        return new Stats(streakDays, totalRecites, masteredCount, totalProgress);
    }

    // ======== 模块掌握度 ========

    private List<ModuleMastery> buildModuleMastery(Long userId) {
        List<KnowledgeModuleEntity> modules = modulePort.listAll();
        List<UserProgressEntity> progressList = progressPort.findByUserId(userId);
        Set<String> masteredIds = progressList.stream()
                .filter(p -> p.getMasteryScore() >= 80)
                .map(UserProgressEntity::getQuestionId)
                .collect(Collectors.toSet());

        List<ModuleMastery> result = new ArrayList<>();
        for (KnowledgeModuleEntity m : modules) {
            int total = questionPort.countByModule(m.getModuleKey());
            int mastered = (int) progressList.stream()
                    .filter(p -> m.getModuleKey().equals(p.getModuleKey())
                            && masteredIds.contains(p.getQuestionId()))
                    .count();
            result.add(new ModuleMastery(
                    m.getModuleKey(), m.getModuleName(), mastered, total));
        }
        return result;
    }

    // ======== 近 7 天趋势 ========

    private List<TrendBar> buildTrend(Long userId) {
        List<ReciteRecordEntity> records = reciteRecordPort.findByUserId(userId, 500);
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> dayCount = records.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));

        String[] dayLabels = {"一", "二", "三", "四", "五", "六", "日"};
        List<TrendBar> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int count = dayCount.getOrDefault(d, 0L).intValue();
            trend.add(new TrendBar(dayLabels[6 - i], count));
        }
        return trend;
    }

    // ======== 最近 3 枚徽章 ========

    private List<BadgeItem> buildBadges(Long userId) {
        Map<String, LocalDateTime> earned = achievementPort.findEarnedBadgeMap(userId);
        return earned.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(e -> new BadgeItem(e.getKey(), e.getKey(), "⭐"))
                .toList();
    }

    // ======== 薄弱标签 ========

    private List<String> buildWeakTags(Long userId) {
        List<LearningJournal> journals = reportPort.findRecentJournals(userId, 5);
        Set<String> tags = new LinkedHashSet<>();
        Type listType = new TypeToken<List<String>>() {}.getType();
        for (LearningJournal j : journals) {
            try {
                JsonObject obj = gson.fromJson(j.getSummaryJson(), JsonObject.class);
                if (obj != null && obj.has("weakTags")) {
                    List<String> list = gson.fromJson(obj.get("weakTags"), listType);
                    if (list != null) tags.addAll(list);
                }
            } catch (Exception ignored) {
                // JSON 解析失败则跳过该条
            }
        }
        return new ArrayList<>(tags);
    }

    // ======== AI 建议 ========

    private String buildAdvice(Long userId) {
        List<LearningJournal> journals = reportPort.findRecentJournals(userId, 1);
        if (journals.isEmpty()) return "开始你的第一次背诵吧！";
        try {
            JsonObject obj = gson.fromJson(journals.get(0).getSummaryJson(), JsonObject.class);
            return obj != null && obj.has("advice") ? obj.get("advice").getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ======== 最近背诵记录 ========

    private List<RecentRecite> buildRecentRecites(Long userId) {
        List<ReciteRecordEntity> records = reciteRecordPort.findByUserId(userId, 200);
        Map<String, String> moduleNames = modulePort.listAll().stream()
                .collect(Collectors.toMap(KnowledgeModuleEntity::getModuleKey,
                        m -> m.getModuleName() != null ? m.getModuleName() : m.getModuleKey(),
                        (a, b) -> a));
        Map<String, List<ReciteRecordEntity>> bySession = records.stream()
                .filter(r -> r.getSessionId() != null)
                .collect(Collectors.groupingBy(ReciteRecordEntity::getSessionId,
                        LinkedHashMap::new, Collectors.toList()));

        List<RecentRecite> result = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, List<ReciteRecordEntity>> entry : bySession.entrySet()) {
            if (count >= 5) break;
            List<ReciteRecordEntity> recs = entry.getValue();
            if (recs.isEmpty()) continue;
            ReciteRecordEntity first = recs.get(0);
            String mk = first.getModuleKey();
            double avg = recs.stream().filter(r -> r.getScore() != null)
                    .mapToInt(ReciteRecordEntity::getScore).average().orElse(0);
            result.add(new RecentRecite(
                    entry.getKey(),
                    formatDateLabel(first.getCreatedAt()),
                    mk, moduleNames.getOrDefault(mk, mk),
                    recs.size(),
                    Math.round(avg * 10.0) / 10.0
            ));
            count++;
        }
        return result;
    }

    private String formatDateLabel(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDate d = dt.toLocalDate();
        LocalDate today = LocalDate.now();
        if (d.equals(today)) return "今天 " + dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (d.equals(today.minusDays(1))) return "昨天 " + dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        return d.format(DateTimeFormatter.ofPattern("M月d日"));
    }

    // ======== 领域值对象（record，不依赖 recite-api） ========

    public record UserInfo(String nickname) {}
    public record Stats(int streakDays, int totalRecites, int masteredCount, int totalProgress) {}
    public record ModuleMastery(String moduleKey, String moduleName, int mastered, int total) {}
    public record TrendBar(String dayLabel, int count) {}
    public record BadgeItem(String key, String name, String icon) {}
    public record RecentRecite(String sessionId, String dateLabel, String moduleKey,
                               String moduleName, int questionCount, double avgScore) {}
    public record HomeDashboardVO(UserInfo user, Stats stats, List<ModuleMastery> moduleMastery,
                                  List<TrendBar> trend, List<BadgeItem> badges,
                                  List<String> weakTags, String advice,
                                  List<RecentRecite> recentRecites) {}
}
