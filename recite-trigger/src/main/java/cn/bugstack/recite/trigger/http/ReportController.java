package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IReportService;
import cn.bugstack.recite.api.dto.DashboardDTO;
import cn.bugstack.recite.api.dto.JournalDetailDTO;
import cn.bugstack.recite.api.dto.JournalItemDTO;
import cn.bugstack.recite.api.dto.ReportStatusDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.report.model.entity.LearningJournal;
import cn.bugstack.recite.domain.report.port.out.ReportPort;
import cn.bugstack.recite.domain.report.service.ReportService;
import cn.bugstack.recite.trigger.config.UserContext;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 报告控制器 — 实现 IReportService 4 端点.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReportController implements IReportService {

    private final ReportService reportService;
    private final ReportPort reportPort;
    private final Gson gson = new Gson();

    @Override
    public Response<DashboardDTO> dashboard() {
        Long userId = UserContext.getUserId();
        var vo = reportService.aggregateDashboard(userId);

        DashboardDTO.StatsCard card = new DashboardDTO.StatsCard(
                vo.statsCard().totalQuestions(), (int) vo.statsCard().avgMastery(),
                vo.statsCard().streakDays(), vo.statsCard().reportCount());

        List<DashboardDTO.TrendPoint> trend = vo.trend().stream().map(t -> {
            String date = t.date() != null ? t.date().format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
            double avgScore = extractDouble(t.summaryJson(), "averageScore");
            return new DashboardDTO.TrendPoint(date, avgScore);
        }).toList();

        DashboardDTO dto = new DashboardDTO(card, vo.moduleBars(), trend, vo.weakTags(), vo.latestAdvice());
        return Response.ok(dto);
    }

    @Override
    public Response<ReportStatusDTO> getBySessionId(String sessionId) {
        LearningJournal journal = reportPort.findBySessionId(sessionId);
        if (journal == null) {
            return Response.ok(new ReportStatusDTO("generating", null));
        }
        JournalDetailDTO detail = toDetailDTO(journal);
        return Response.ok(new ReportStatusDTO("done", detail));
    }

    @Override
    public Response<List<JournalItemDTO>> journal(int page, int size) {
        Long userId = UserContext.getUserId();
        List<LearningJournal> journals = reportPort.findJournals(userId, page, size);
        List<JournalItemDTO> dtos = journals.stream().map(j -> {
            String summary = extractString(j.getSummaryJson(), "summary");
            return new JournalItemDTO(j.getId(), j.getCreatedAt(),
                    summary != null ? summary : "");
        }).toList();
        return Response.ok(dtos);
    }

    @Override
    public Response<JournalDetailDTO> journalDetail(Long id) {
        LearningJournal journal = reportPort.findById(id);
        if (journal == null) {
            return Response.fail("404", "档案不存在");
        }
        return Response.ok(toDetailDTO(journal));
    }

    // ---- JSON 解析辅助 ----

    private JournalDetailDTO toDetailDTO(LearningJournal journal) {
        String json = journal.getSummaryJson();
        JournalDetailDTO dto = new JournalDetailDTO();
        dto.setId(journal.getId());
        dto.setSessionId(journal.getSessionId());
        dto.setTotalScore(extractDouble(json, "totalScore"));
        dto.setAverageScore(extractDouble(json, "averageScore"));
        dto.setTotalQuestions(extractInt(json, "totalQuestions"));
        dto.setStrengths(extractStringList(json, "strengths"));
        dto.setWeaknesses(extractStringList(json, "weaknesses"));
        dto.setAdvice(extractString(json, "advice"));
        dto.setTrendComment(extractString(json, "trendComment"));
        dto.setWeakTags(extractStringList(json, "weakTags"));
        dto.setCreatedAt(journal.getCreatedAt());

        // moduleScores
        try {
            Map<String, Object> map = gson.fromJson(json,
                    new TypeToken<Map<String, Object>>() {}.getType());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) map.get("moduleScores");
            if (rawList != null) {
                dto.setModuleScores(rawList.stream().map(m -> {
                    JournalDetailDTO.ModuleScore ms = new JournalDetailDTO.ModuleScore();
                    ms.setModuleKey((String) m.getOrDefault("moduleKey", ""));
                    ms.setModuleName((String) m.getOrDefault("moduleName", ""));
                    ms.setAvgScore(((Number) m.getOrDefault("avgScore", 0)).doubleValue());
                    ms.setCount(((Number) m.getOrDefault("count", 0)).intValue());
                    return ms;
                }).toList());
            }
        } catch (Exception e) {
            log.warn("解析 moduleScores 失败: id={}", journal.getId(), e);
        }
        return dto;
    }

    private String extractString(String json, String key) {
        try {
            Map<String, Object> map = gson.fromJson(json,
                    new TypeToken<Map<String, Object>>() {}.getType());
            Object val = map.get(key);
            return val != null ? val.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private double extractDouble(String json, String key) {
        try {
            Map<String, Object> map = gson.fromJson(json,
                    new TypeToken<Map<String, Object>>() {}.getType());
            Object val = map.get(key);
            return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int extractInt(String json, String key) {
        try {
            Map<String, Object> map = gson.fromJson(json,
                    new TypeToken<Map<String, Object>>() {}.getType());
            Object val = map.get(key);
            return val instanceof Number ? ((Number) val).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(String json, String key) {
        try {
            Map<String, Object> map = gson.fromJson(json,
                    new TypeToken<Map<String, Object>>() {}.getType());
            Object val = map.get(key);
            return val instanceof List ? (List<String>) val : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}
