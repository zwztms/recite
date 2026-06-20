package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IAchievementService;
import cn.bugstack.recite.api.dto.BadgeDTO;
import cn.bugstack.recite.api.dto.BadgeDetailDTO;
import cn.bugstack.recite.api.dto.NewBadgeDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeDefinition;
import cn.bugstack.recite.domain.achievement.model.valueobj.BadgeProgress;
import cn.bugstack.recite.domain.achievement.port.out.AchievementPort;
import cn.bugstack.recite.domain.achievement.port.out.NewBadgePort;
import cn.bugstack.recite.domain.achievement.service.AchievementService;
import cn.bugstack.recite.domain.achievement.service.BadgeRegistry;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.trigger.config.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成就控制器 — 19 枚模块徽章.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AchievementController implements IAchievementService {

    private final AchievementService achievementService;
    private final AchievementPort achievementPort;
    private final NewBadgePort newBadgePort;
    private final ReciteRecordPort reciteRecordPort;

    @Override
    public Response<List<BadgeDTO>> listAll() {
        Long userId = UserContext.getUserId();
        Map<String, LocalDateTime> earnedMap = achievementPort.findEarnedBadgeMap(userId);

        List<BadgeDTO> dtos = new ArrayList<>();
        for (BadgeDefinition badge : BadgeRegistry.ALL_BADGES) {
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

            // 进度：已背诵数 / 模块总题数
            int recited = reciteRecordPort.countByModule(userId, badge.getKey());
            int total = getModuleTotal(badge.getKey());
            int pct = total > 0 ? Math.min(100, recited * 100 / total) : 0;
            dto.setProgress(new BadgeDTO.Progress(recited, total, pct));

            dtos.add(dto);
        }

        return Response.ok(dtos);
    }

    @Override
    public Response<BadgeDetailDTO> getDetail(String badgeKey) {
        Long userId = UserContext.getUserId();
        BadgeDefinition badge = BadgeRegistry.getByKey(badgeKey);
        if (badge == null) return Response.fail("404", "徽章不存在");

        Map<String, LocalDateTime> earnedMap = achievementPort.findEarnedBadgeMap(userId);

        BadgeDetailDTO dto = new BadgeDetailDTO();
        dto.setKey(badge.getKey());
        dto.setName(badge.getName());
        dto.setDescription(badge.getDescription());
        dto.setIcon(badge.getIcon());
        dto.setCategory(badge.getCategory());
        dto.setHidden(badge.isHidden());
        dto.setEarned(earnedMap.containsKey(badgeKey));
        dto.setEarnedAt(earnedMap.get(badgeKey));
        dto.setEarnCondition(badge.getDescription());
        dto.setDetailedDescription(badge.getDescription());

        int recited = reciteRecordPort.countByModule(userId, badgeKey);
        int total = getModuleTotal(badgeKey);
        int pct = total > 0 ? Math.min(100, recited * 100 / total) : 0;
        dto.setProgressPercent(pct);

        return Response.ok(dto);
    }

    @Override
    public Response<List<NewBadgeDTO>> getNewBadges() {
        Long userId = UserContext.getUserId();
        List<String> keys = newBadgePort.getNewBadges(userId);
        List<NewBadgeDTO> dtos = keys.stream().map(key -> {
            BadgeDefinition badge = BadgeRegistry.getByKey(key);
            if (badge == null) return null;
            return new NewBadgeDTO(badge.getKey(), badge.getName(), badge.getDescription(), badge.getIcon());
        }).filter(Objects::nonNull).toList();
        return Response.ok(dtos);
    }

    @Override
    public Response<Void> ackNewBadges() {
        Long userId = UserContext.getUserId();
        newBadgePort.ackNewBadges(userId);
        return Response.ok();
    }

    /** 模块总题数（与 AchievementConsumer.MODULE_TOTAL_QUESTIONS 保持一致） */
    private static int getModuleTotal(String key) {
        return switch (key) {
            case "java-basics" -> 69; case "juc" -> 61; case "jvm" -> 32;
            case "java-collections" -> 42; case "spring" -> 58; case "mysql" -> 89;
            case "redis" -> 43; case "os" -> 74; case "ds-algo" -> 33;
            case "network" -> 76; case "ai-rag" -> 65; case "ai-spring" -> 9;
            case "ai-finetune" -> 21; case "ai-prompt" -> 6; case "ai-eval" -> 5;
            case "ai-security" -> 3; case "ai-design" -> 6; case "ai-openclaw" -> 28;
            case "ai-agent" -> 19;
            default -> 20;
        };
    }
}
