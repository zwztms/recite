package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.DashboardDTO;
import cn.bugstack.recite.api.dto.JournalDetailDTO;
import cn.bugstack.recite.api.dto.JournalItemDTO;
import cn.bugstack.recite.api.dto.ReportStatusDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报告 REST 接口 — 4 端点.
 */
@RequestMapping("/report")
public interface IReportService {

    /** 个人仪表盘 */
    @GetMapping("/dashboard")
    Response<DashboardDTO> dashboard();

    /** 轮询报告状态（前端每 2 秒调） */
    @GetMapping("/{sessionId}")
    Response<ReportStatusDTO> getBySessionId(@PathVariable String sessionId);

    /** 学习档案列表 */
    @GetMapping("/journal")
    Response<List<JournalItemDTO>> journal(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size);

    /** 单次档案详情 */
    @GetMapping("/journal/{id}")
    Response<JournalDetailDTO> journalDetail(@PathVariable Long id);
}
