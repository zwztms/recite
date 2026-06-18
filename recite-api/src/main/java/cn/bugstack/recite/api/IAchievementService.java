package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.BadgeDTO;
import cn.bugstack.recite.api.dto.BadgeDetailDTO;
import cn.bugstack.recite.api.dto.NewBadgeDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成就 REST 接口 — 4 端点.
 */
@RequestMapping("/achievement")
public interface IAchievementService {

    /** 全部徽章 + 进度（徽章墙） */
    @GetMapping("/")
    Response<List<BadgeDTO>> listAll();

    /** 单枚徽章详情 */
    @GetMapping("/{badgeKey}")
    Response<BadgeDetailDTO> getDetail(@PathVariable String badgeKey);

    /** 轮询新徽章（finishRecite 后每 2 秒） */
    @GetMapping("/new")
    Response<List<NewBadgeDTO>> getNewBadges();

    /** 确认已读，删除 Redis 新徽章标记 */
    @PostMapping("/new/ack")
    Response<Void> ackNewBadges();
}
