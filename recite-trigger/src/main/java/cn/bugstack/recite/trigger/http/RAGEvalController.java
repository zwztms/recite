package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.knowledge.port.out.EvaluationRecordPort;
import cn.bugstack.recite.domain.knowledge.port.out.EvaluationRecordPort.EvalRecord;
import cn.dev33.satoken.annotation.SaCheckRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 评估 Debug API（管理员）.
 */
@RestController
@RequestMapping("/admin/rag")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class RAGEvalController {

    private final EvaluationRecordPort evalRecordPort;

    /** 查看某次背诵的 RAGAS 四指标 */
    @GetMapping("/eval/{sessionId}")
    public cn.bugstack.recite.api.response.Response<EvalRecord> getEval(
            @PathVariable String sessionId) {
        EvalRecord record = evalRecordPort.findBySessionId(sessionId);
        return record != null
                ? cn.bugstack.recite.api.response.Response.ok(record)
                : cn.bugstack.recite.api.response.Response.fail("404", "评估记录不存在");
    }

    /** 最近 N 次评估统计 */
    @GetMapping("/stats")
    public cn.bugstack.recite.api.response.Response<List<EvalRecord>> getStats(
            @RequestParam(defaultValue = "20") int limit) {
        return cn.bugstack.recite.api.response.Response.ok(
                evalRecordPort.getRecentStats(limit));
    }
}
