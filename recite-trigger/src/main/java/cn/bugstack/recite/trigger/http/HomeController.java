package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IHomeService;
import cn.bugstack.recite.api.dto.HomeDashboardDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.home.service.HomeService;
import cn.bugstack.recite.trigger.config.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人主页控制器 — 实现 IHomeService 1 端点.
 *
 * <p>调用域服务 → 拿 VO → 转换为 API DTO → 包装 Response.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HomeController implements IHomeService {

    private final HomeService homeService;

    @Override
    public Response<HomeDashboardDTO> dashboard() {
        Long userId = UserContext.getUserId();
        var vo = homeService.build(userId);

        // VO → DTO 转换
        HomeDashboardDTO dto = new HomeDashboardDTO();
        dto.setUser(new HomeDashboardDTO.UserInfo(vo.user().nickname()));
        dto.setStats(new HomeDashboardDTO.Stats(
                vo.stats().streakDays(), vo.stats().totalRecites(),
                vo.stats().masteredCount(), vo.stats().totalProgress()));
        dto.setModuleMastery(vo.moduleMastery().stream()
                .map(m -> new HomeDashboardDTO.ModuleMastery(
                        m.moduleKey(), m.moduleName(), m.mastered(), m.total()))
                .toList());
        dto.setTrend(vo.trend().stream()
                .map(t -> new HomeDashboardDTO.TrendBar(t.dayLabel(), t.count()))
                .toList());
        dto.setBadges(vo.badges().stream()
                .map(b -> new HomeDashboardDTO.BadgeItem(b.key(), b.name(), b.icon()))
                .toList());
        dto.setWeakTags(vo.weakTags());
        dto.setAdvice(vo.advice());
        dto.setRecentRecites(vo.recentRecites().stream()
                .map(r -> new HomeDashboardDTO.RecentRecite(
                        r.sessionId(), r.dateLabel(), r.moduleKey(), r.moduleName(),
                        r.questionCount(), r.avgScore()))
                .toList());

        return Response.ok(dto);
    }
}
