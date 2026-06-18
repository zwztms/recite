package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IAchievementService;
import cn.bugstack.recite.api.dto.BadgeDTO;
import cn.bugstack.recite.api.dto.BadgeDetailDTO;
import cn.bugstack.recite.api.dto.NewBadgeDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeDefinition;
import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeProgress;
import cn.bugstack.recite.domain.achievement.model.valueobj.UserStatsVO;
import cn.bugstack.recite.domain.achievement.port.out.AchievementPort;
import cn.bugstack.recite.domain.achievement.port.out.NewBadgePort;
import cn.bugstack.recite.domain.achievement.service.AchievementService;
import cn.bugstack.recite.domain.achievement.service.BadgeRegistry;
import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.trigger.config.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成就控制器 — 实现 IAchievementService 4 端点.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AchievementController implements IAchievementService {

    private final AchievementService achievementService;
    private final AchievementPort achievementPort;
    private final NewBadgePort newBadgePort;
    private final ReciteRecordPort reciteRecordPort;
    private final ProgressPort progressPort;
    private final StreakPort streakPort;

    // ================================================================
    // GET /achievement/ — 徽章墙
    // ================================================================

    @Override
    public Response<List<BadgeDTO>> listAll() {
        Long userId = UserContext.getUserId();
        Map<String, LocalDateTime> earnedMap = achievementPort.findEarnedBadgeMap(userId);
        UserStatsVO stats = buildLightStats(userId, earnedMap);

        // 逐模块背诵次数（仅未获得模块徽章的才查）
        Map<String, Integer> moduleCounts = new HashMap<>();
        for (BadgeDefinition badge : BadgeRegistry.getPublicBadges()) {
            if (isModuleBadge(badge.getKey()) && !earnedMap.containsKey(badge.getKey())) {
                moduleCounts.put(badge.getKey(),
                        reciteRecordPort.countByModule(userId, badge.getKey()));
            }
        }

        List<BadgeDTO> dtos = new ArrayList<>();
        for (BadgeDefinition badge : BadgeRegistry.getPublicBadges()) {
            BadgeDTO dto = new BadgeDTO();
            dto.setKey(badge.getKey());
            dto.setName(badge.getName());
            dto.setDescription(badge.getDescription());
            dto.setIcon(badge.getIcon());
            dto.setCategory(badge.getCategory());
            dto.setHidden(badge.isHidden());

            if (earnedMap.containsKey(badge.getKey())) {
                dto.setEarned(true);
                dto.setEarnedAt(earnedMap.get(badge.getKey()));
            }

            BadgeProgress p = achievementService.calculateProgress(badge, stats, moduleCounts);
            dto.setProgress(new BadgeDTO.Progress(p.getCurrent(), p.getTarget(), p.getPercent()));

            dtos.add(dto);
        }

        return Response.ok(dtos);
    }

    // ================================================================
    // GET /achievement/{badgeKey} — 徽章详情
    // ================================================================

    @Override
    public Response<BadgeDetailDTO> getDetail(String badgeKey) {
        Long userId = UserContext.getUserId();
        BadgeDefinition badge = BadgeRegistry.getByKey(badgeKey);
        if (badge == null) {
            return Response.fail("404", "徽章不存在");
        }

        Map<String, LocalDateTime> earnedMap = achievementPort.findEarnedBadgeMap(userId);
        UserStatsVO stats = buildLightStats(userId, earnedMap);

        BadgeDetailDTO dto = new BadgeDetailDTO();
        dto.setKey(badge.getKey());
        dto.setName(badge.getName());
        dto.setDescription(badge.getDescription());
        dto.setIcon(badge.getIcon());
        dto.setCategory(badge.getCategory());
        dto.setHidden(badge.isHidden());
        dto.setEarned(earnedMap.containsKey(badgeKey));
        dto.setEarnedAt(earnedMap.get(badgeKey));
        dto.setEarnCondition(badge.isHidden() ? "???" : badge.getDescription());
        dto.setDetailedDescription(badge.isHidden() ? "隐藏徽章，条件不公开" : badge.getDescription());

        BadgeProgress p = achievementService.calculateProgress(badge, stats, Map.of());
        dto.setProgressPercent(p.getPercent());

        return Response.ok(dto);
    }

    // ================================================================
    // GET /achievement/new — 轮询新徽章
    // ================================================================

    @Override
    public Response<List<NewBadgeDTO>> getNewBadges() {
        Long userId = UserContext.getUserId();
        List<String> keys = newBadgePort.getNewBadges(userId);
        List<NewBadgeDTO> dtos = keys.stream().map(key -> {
            BadgeDefinition badge = BadgeRegistry.getByKey(key);
            if (badge == null) return null;
            return new NewBadgeDTO(badge.getKey(), badge.getName(),
                    badge.getDescription(), badge.getIcon());
        }).filter(Objects::nonNull).toList();
        return Response.ok(dtos);
    }

    // ================================================================
    // POST /achievement/new/ack — 确认已读
    // ================================================================

    @Override
    public Response<Void> ackNewBadges() {
        Long userId = UserContext.getUserId();
        newBadgePort.ackNewBadges(userId);
        return Response.ok();
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    private static final Set<String> MODULE_KEYS = Set.of(
            "java-basics", "juc", "jvm", "java-collections", "spring", "mysql", "redis",
            "os", "ds-algo", "network",
            "ai-rag", "ai-spring", "ai-finetune", "ai-prompt", "ai-eval", "ai-security",
            "ai-design", "ai-openclaw", "ai-agent"
    );

    private boolean isModuleBadge(String key) {
        return MODULE_KEYS.contains(key);
    }

    /** 组装轻量统计（仅用于进度计算，不含会话级字段） */
    private UserStatsVO buildLightStats(Long userId, Map<String, LocalDateTime> earnedMap) {
        int totalRecites = reciteRecordPort.countByUserId(userId);
        double avgScore = reciteRecordPort.avgScoreByUserId(userId);
        int perfectScoreCount = reciteRecordPort.countPerfectScores(userId);
        int totalSessions = reciteRecordPort.countSessionsByUserId(userId);
        int masteredCount = progressPort.countMastered(userId);

        Optional<UserStreakEntity> streakOpt = streakPort.findByUserId(userId);
        int currentStreak = streakOpt.map(UserStreakEntity::getCurrentStreak).orElse(0);
        int longestStreak = streakOpt.map(UserStreakEntity::getLongestStreak).orElse(0);

        Set<String> earnedKeys = earnedMap.keySet();
        Set<String> earnedModules = earnedKeys.stream()
                .filter(MODULE_KEYS::contains).collect(Collectors.toSet());

        boolean wasStreakBroken = longestStreak > 0 && currentStreak < longestStreak;

        return new UserStatsVO(totalRecites, avgScore, perfectScoreCount,
                currentStreak, longestStreak, masteredCount, totalSessions,
                earnedModules, earnedKeys, 0, 0, 0, 0, 0, wasStreakBroken);
    }
}
