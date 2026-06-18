package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IReciteService;
import cn.bugstack.recite.api.dto.*;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.model.entity.ReciteSession;
import cn.bugstack.recite.domain.recite.model.valueobj.ScoreResultVO;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.domain.recite.port.out.ReciteSessionPort;
import cn.bugstack.recite.domain.recite.service.ReciteOrchestrationService;
import cn.bugstack.recite.trigger.config.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 背诵控制器 — 实现 IReciteService 6 端点.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReciteController implements IReciteService {

    private final ReciteOrchestrationService orchestrationService;
    private final ReciteSessionPort sessionPort;
    private final ReciteRecordPort recordPort;
    private final QuestionPort questionPort;

    /** SSE 推送线程池（Step 6 submitAnswer 用） */
    private final ThreadPoolTaskExecutor sseExecutor = sseExecutor();

    private static ThreadPoolTaskExecutor sseExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(10);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("sse-");
        exec.initialize();
        return exec;
    }

    // ==== 开始背诵 ====

    @Override
    public Response<ReciteStartResultDTO> startRecite(ReciteStartRequestDTO request) {
        Long userId = UserContext.getUserId();
        cn.bugstack.recite.types.enums.ReciteMode mode =
                cn.bugstack.recite.types.enums.ReciteMode.valueOf(request.getMode().toUpperCase());

        ReciteSession session = orchestrationService.startRecite(userId, mode,
                request.getModuleKeys(), request.getCount());

        // 查首题完整内容
        QuestionDTO firstQuestion = null;
        if (session.getCurrentQuestionId() != null) {
            QuestionEntity q = questionPort.getById(session.getCurrentQuestionId());
            if (q != null) {
                firstQuestion = new QuestionDTO();
                firstQuestion.setId(q.getId());
                firstQuestion.setQuestion(q.getQuestion());
                firstQuestion.setContent(q.getContent());
                firstQuestion.setModuleKey(q.getModuleKey());
                firstQuestion.setCategory(q.getCategory());
                firstQuestion.setTags(q.getTags());
                firstQuestion.setDifficulty(q.getDifficulty());
            }
        }

        ReciteStartResultDTO result = new ReciteStartResultDTO();
        result.setSessionId(session.getSessionId());
        result.setQuestion(firstQuestion);
        result.setQuestionIndex(1);
        result.setTotalQuestions(session.getTotalQuestions());
        return Response.ok(result);
    }

    // ==== 后续端点（Step 6-9 实现） ====

    @Override
    public SseEmitter submitAnswer(String sid, SubmitAnswerRequestDTO request) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(60_000L);

        sseExecutor.execute(() -> {
            try {
                ScoreResultVO vo = orchestrationService.submitAnswer(
                        userId, sid, request.getQuestionId(), request.getAnswer());

                // SSE 分 6 段推送，每段间隔 400ms
                sendSse(emitter, "score", Map.of("score", vo.score()), 0);
                sendSse(emitter, "correct", Map.of("points", vo.correctPoints()), 400);
                sendSse(emitter, "missed", Map.of("points", vo.missedPoints()), 400);
                sendSse(emitter, "suggestion", Map.of("text", vo.suggestion()), 400);
                sendSse(emitter, "followUp", Map.of("question",
                        vo.followUpQuestion() != null ? vo.followUpQuestion() : ""), 400);
                sendSse(emitter, "done", Map.of(), 400);

                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 评分异常: sid={}", sid, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", e.getMessage())));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendSse(SseEmitter emitter, String event, Object data, long delayMs) {
        try {
            if (delayMs > 0) Thread.sleep(delayMs);
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            log.error("SSE 推送失败: event={}", event, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Response<String> submitFollowUp(String sid, FollowUpRequestDTO request) {
        Long userId = UserContext.getUserId();
        String feedback = orchestrationService.submitFollowUp(
                userId, sid, request.getRecordId(), request.getFollowUpAnswer());
        return Response.ok(feedback);
    }

    @Override
    public Response<SessionReportDTO> finishRecite(String sid) {
        Long userId = UserContext.getUserId();
        var vo = orchestrationService.finishRecite(userId, sid);
        SessionReportDTO dto = new SessionReportDTO();
        dto.setTotalScore(vo.totalScore());
        dto.setAverageScore(vo.averageScore());
        dto.setTotalQuestions(vo.totalQuestions());
        dto.setStrengths(vo.strengths());
        dto.setWeaknesses(vo.weaknesses());
        dto.setAdvice(vo.advice());
        return Response.ok(dto);
    }

    @Override
    public Response<ReciteSessionDTO> getSession(String sid) {
        var opt = sessionPort.findById(sid);
        if (opt.isEmpty()) {
            return Response.fail("404", "会话不存在");
        }
        ReciteSession s = opt.get();
        ReciteSessionDTO dto = new ReciteSessionDTO();
        dto.setSessionId(s.getSessionId());
        dto.setMode(s.getMode().name());
        dto.setCurrentIndex(s.getCurrentIndex() + 1); // 1-based
        dto.setTotalQuestions(s.getTotalQuestions());
        dto.setStatus(s.getStatus());
        return Response.ok(dto);
    }

    @Override
    public Response<QuestionDTO> getCurrentQuestion(String sid) {
        Long userId = UserContext.getUserId();
        var sessionOpt = sessionPort.findById(sid);
        if (sessionOpt.isEmpty()) {
            return Response.fail("404", "会话不存在");
        }
        ReciteSession session = sessionOpt.get();
        if (session.getCurrentQuestionId() == null) {
            return Response.fail("404", "没有当前题目");
        }
        QuestionEntity q = questionPort.getById(session.getCurrentQuestionId());
        if (q == null) {
            return Response.fail("404", "题目不存在");
        }
        QuestionDTO dto = new QuestionDTO();
        dto.setId(q.getId());
        dto.setQuestion(q.getQuestion());
        dto.setContent(q.getContent());
        dto.setModuleKey(q.getModuleKey());
        dto.setCategory(q.getCategory());
        dto.setTags(q.getTags());
        dto.setDifficulty(q.getDifficulty());
        return Response.ok(dto);
    }

    @Override
    public Response<List<ReciteRecordDTO>> getHistory(int limit) {
        Long userId = UserContext.getUserId();
        List<ReciteRecordEntity> records = recordPort.findByUserId(userId, limit);
        List<ReciteRecordDTO> dtos = records.stream().map(r -> {
            ReciteRecordDTO d = new ReciteRecordDTO();
            d.setId(r.getId());
            d.setSessionId(r.getSessionId());
            d.setMode(r.getMode());
            d.setScore(r.getScore());
            d.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            return d;
        }).toList();
        return Response.ok(dtos);
    }
}
