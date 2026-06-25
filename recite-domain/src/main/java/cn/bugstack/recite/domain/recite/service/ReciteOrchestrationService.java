package cn.bugstack.recite.domain.recite.service;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.model.valueobj.EmbeddedQuestionVO;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.domain.progress.service.SpacedRepetitionService;
import cn.bugstack.recite.domain.progress.service.StreakService;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.model.entity.ReciteSession;
import cn.bugstack.recite.domain.recite.model.valueobj.ScoreResultVO;
import cn.bugstack.recite.domain.recite.model.valueobj.SessionReportVO;
import cn.bugstack.recite.domain.recite.port.out.LlmPort;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.domain.recite.port.out.AchievementMessagePort;
import cn.bugstack.recite.domain.recite.port.out.ReciteSessionPort;
import cn.bugstack.recite.domain.recite.port.out.ReportMessagePort;
import cn.bugstack.recite.domain.recite.port.out.ScoreSlotPort;
import cn.bugstack.recite.domain.recite.port.out.SkillPort;
import cn.bugstack.recite.domain.recite.port.out.SkillSlotPort;
import cn.bugstack.recite.types.annotation.ReciteTraceRoot;
import cn.bugstack.recite.types.enums.ReciteMode;
import cn.bugstack.recite.types.skill.SkillResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 背诵核心编排器 — 不调外部 API，全部通过 Port 接口编排.
 * Phase RAG: submitAnswer 接入 7 阶段知识增强管线.
 */
@Slf4j
@Service
public class ReciteOrchestrationService {

    private final QuestionPort questionPort;
    private final ReciteSessionPort sessionPort;
    private final ScoreSlotPort scoreSlotPort;
    private final LlmPort llmPort;
    private final ReciteRecordPort recordPort;
    private final ReciteGateService gateService;
    private final SpacedRepetitionService spacedRepetitionService;
    private final ProgressPort progressPort;
    private final StreakService streakService;
    private final ReportMessagePort reportMessagePort;
    private final AchievementMessagePort achievementMessagePort;
    private final SkillPort skillPort;
    private final SkillSlotPort skillSlotPort;

    // ==== RAG 管线封装 ====
    private final ReciteKnowledgeService knowledgeService;

    public ReciteOrchestrationService(QuestionPort questionPort,
                                       ReciteSessionPort sessionPort,
                                       ScoreSlotPort scoreSlotPort,
                                       LlmPort llmPort,
                                       ReciteRecordPort recordPort,
                                       ReciteGateService gateService,
                                       SpacedRepetitionService spacedRepetitionService,
                                       ProgressPort progressPort,
                                       StreakService streakService,
                                       ReportMessagePort reportMessagePort,
                                       AchievementMessagePort achievementMessagePort,
                                       SkillPort skillPort,
                                       SkillSlotPort skillSlotPort,
                                       ReciteKnowledgeService knowledgeService) {
        this.questionPort = questionPort;
        this.sessionPort = sessionPort;
        this.scoreSlotPort = scoreSlotPort;
        this.llmPort = llmPort;
        this.recordPort = recordPort;
        this.gateService = gateService;
        this.spacedRepetitionService = spacedRepetitionService;
        this.progressPort = progressPort;
        this.streakService = streakService;
        this.reportMessagePort = reportMessagePort;
        this.achievementMessagePort = achievementMessagePort;
        this.skillPort = skillPort;
        this.skillSlotPort = skillSlotPort;
        this.knowledgeService = knowledgeService;
    }

    /** 开始背诵 — 拉题 + 创建会话 */
    public ReciteSession startRecite(Long userId, ReciteMode mode,
                                      List<String> moduleKeys, int count) {
        // 1. 按模式拉题
        List<EmbeddedQuestionVO> vos;
        switch (mode) {
            case CATEGORY -> {
                String key = (moduleKeys != null && !moduleKeys.isEmpty())
                        ? moduleKeys.get(0) : "";
                vos = questionPort.searchRandom(
                        key.isEmpty() ? List.of() : List.of(key), count);
            }
            case RANDOM -> vos = questionPort.searchRandom(moduleKeys, count);
            case REVIEW -> {
                List<UserProgressEntity> dueItems = progressPort.findDueQuestions(userId, count);
                if (dueItems.isEmpty()) {
                    throw new cn.bugstack.recite.domain.recite.exception.ReciteException(
                            cn.bugstack.recite.types.enums.ResponseCode.NOT_FOUND.getCode(),
                            "暂无到期复习题目");
                }
                vos = new ArrayList<>();
                for (UserProgressEntity p : dueItems) {
                    QuestionEntity q = questionPort.getById(p.getQuestionId());
                    if (q != null) {
                        vos.add(new EmbeddedQuestionVO(q, p.getMasteryScore() / 100.0));
                    }
                }
            }
            default -> throw new IllegalArgumentException("未知背诵模式: " + mode);
        }

        // 2. 构建会话
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        List<String> questionIds = vos.stream()
                .map(vo -> vo.question().getId())
                .toList();

        ReciteSession session = ReciteSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .mode(mode)
                .moduleKeys(moduleKeys)
                .currentQuestionId(questionIds.isEmpty() ? null : questionIds.get(0))
                .questionIds(questionIds)
                .currentIndex(0)
                .totalQuestions(questionIds.size())
                .status("ACTIVE")
                .followUpDepth(0)
                .createdAt(LocalDateTime.now())
                .build();

        // 3. 存 Redis
        sessionPort.save(session);
        log.info("创建背诵会话: userId={}, mode={}, sid={}, 题目数={}", userId, mode, sessionId, questionIds.size());

        return session;
    }

    /** 提交答案 → 校验 + RAG管线 + LLM评分 + 存记录 + 更新会话 */
    @ReciteTraceRoot("submitAnswer")
    public ScoreResultVO submitAnswer(Long userId, String sessionId,
                                       String questionId, String answer) {
        // 1. 校验
        ReciteSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new cn.bugstack.recite.domain.recite.exception.ReciteException(
                        cn.bugstack.recite.types.enums.ResponseCode.SESSION_NOT_FOUND.getCode(),
                        "背诵会话不存在或已过期"));
        gateService.validateSession(session, userId);
        gateService.validateAnswer(answer);

        // 2. 查题
        QuestionEntity question = questionPort.getById(questionId);
        if (question == null) {
            throw new cn.bugstack.recite.domain.recite.exception.ReciteException(
                    cn.bugstack.recite.types.enums.ResponseCode.QUESTION_NOT_FOUND.getCode(),
                    "题目不存在");
        }

        // ==== Phase RAG: 知识增强检索 ====
        List<String> knowledgeRefs = knowledgeService.retrieve(question, answer, sessionId);

        // 3. 评分（REVIEW 自评 vs 其他 LLM 评分 + Skill 扩展）
        ScoreResultVO vo;
        List<SkillResultVO> skillResults = List.of();
        List<LlmPort.ToolCallRequest> pendingToolCalls = List.of();

        if (session.getMode() == cn.bugstack.recite.types.enums.ReciteMode.REVIEW) {
            int score = mapSelfAssessment(answer);
            vo = new ScoreResultVO(score, List.of(), List.of(), "", "");
        } else {
            List<SkillPort.ToolDefinition> toolDefs = skillPort.listToolDefinitions();

            if (toolDefs.isEmpty()) {
                // 无 skill：走评分逻辑（含知识参考）
                boolean acquired = scoreSlotPort.tryAcquire(30_000);
                if (!acquired) {
                    throw new cn.bugstack.recite.domain.recite.exception.ReciteException(
                            cn.bugstack.recite.types.enums.ResponseCode.LLM_TIMEOUT.getCode(),
                            "评分排队超时，请稍后重试");
                }
                try {
                    vo = llmPort.score(question, answer, knowledgeRefs);
                    gateService.validateScore(vo.score());
                } finally {
                    scoreSlotPort.release();
                }
            } else {
                // 有 skill：构建 tools → scoreWithSkills
                List<Map<String, Object>> tools = toolDefs.stream().map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("type", "function");
                    Map<String, Object> f = new HashMap<>();
                    f.put("name", t.name());
                    f.put("description", t.description());
                    f.put("parameters", t.parameters());
                    m.put("function", f);
                    return m;
                }).toList();

                boolean acquired = scoreSlotPort.tryAcquire(30_000);
                if (!acquired) {
                    throw new cn.bugstack.recite.domain.recite.exception.ReciteException(
                            cn.bugstack.recite.types.enums.ResponseCode.LLM_TIMEOUT.getCode(),
                            "评分排队超时，请稍后重试");
                }
                try {
                    LlmPort.EnhancedScoreResult enhanced = llmPort.scoreWithSkills(question, answer, tools);
                    vo = new ScoreResultVO(enhanced.score(), enhanced.correctPoints(),
                            enhanced.missedPoints(), enhanced.suggestion(), enhanced.followUpQuestion());
                    gateService.validateScore(vo.score());
                    pendingToolCalls = enhanced.toolCalls() != null ? enhanced.toolCalls() : List.of();
                } finally {
                    scoreSlotPort.release();
                }

                // 评分槽释放后，在独立 skill 槽中执行 tool calls
                if (!pendingToolCalls.isEmpty()) {
                    skillResults = executeSkills(pendingToolCalls, question, answer);
                }
                vo = new ScoreResultVO(vo.score(), vo.correctPoints(), vo.missedPoints(),
                        vo.suggestion(), vo.followUpQuestion(), skillResults);
            }
        }

        // 6. 存背诵记录
        ReciteRecordEntity record = ReciteRecordEntity.builder()
                .userId(userId)
                .sessionId(sessionId)
                .mode(session.getMode().name())
                .moduleKey(question.getModuleKey())
                .questionId(questionId)
                .userAnswer(answer)
                .score(vo.score())
                .feedback(String.join(";", vo.correctPoints() != null ? vo.correctPoints() : List.of()))
                .followUpQuestion(vo.followUpQuestion())
                .followUpDepth(0)
                .build();
        recordPort.save(record);

        // 7. 更新间隔重复掌握度
        Optional<UserProgressEntity> current = progressPort.findByUserAndQuestion(userId, questionId);
        UserProgressEntity updated = spacedRepetitionService.calculateAfterScore(
                current.orElse(null), vo.score(), userId, questionId, question.getModuleKey());
        if (current.isPresent()) {
            updated.setId(current.get().getId());
            progressPort.update(updated);
        } else {
            progressPort.save(updated);
        }

        // ⑦ 记录 RAG 上下文记忆 + 异步评估
        knowledgeService.recordMemory(sessionId, question.getQuestion(), answer,
                vo.missedPoints(), question.getModuleKey());
        knowledgeService.evaluateAsync(sessionId, question.getQuestion(), answer,
                vo.suggestion(), knowledgeRefs);

        // 8. 更新会话
        session.setCurrentIndex(session.getCurrentIndex() + 1);
        session.setFollowUpDepth(0);
        if (session.getCurrentIndex() < session.getTotalQuestions()) {
            session.setCurrentQuestionId(session.getQuestionIds().get(session.getCurrentIndex()));
        }
        sessionPort.update(session);

        return vo;
    }

    /** 追问回答 → 校验深度 + LLM 追问 + 更新记录 */
    public String submitFollowUp(Long userId, String sessionId,
                                  Long recordId, String followUpAnswer) {
        ReciteSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new cn.bugstack.recite.domain.recite.exception.ReciteException(
                        cn.bugstack.recite.types.enums.ResponseCode.SESSION_NOT_FOUND.getCode(),
                        "背诵会话不存在或已过期"));
        gateService.validateSession(session, userId);
        gateService.validateFollowUpDepth(session.getFollowUpDepth());

        ReciteRecordEntity record = recordPort.findById(recordId);
        if (record == null) {
            throw new cn.bugstack.recite.domain.recite.exception.ReciteException("404", "记录不存在");
        }
        QuestionEntity question = questionPort.getById(record.getQuestionId());

        String feedback = llmPort.followUp(question, followUpAnswer);
        recordPort.updateFollowUp(recordId, followUpAnswer, feedback);

        session.setFollowUpDepth(session.getFollowUpDepth() + 1);
        sessionPort.update(session);

        return feedback;
    }

    /** 结束背诵 → Java 统计 + MQ 异步报告 + 清理记忆 */
    @ReciteTraceRoot("finishRecite")
    public SessionReportVO finishRecite(Long userId, String sessionId) {
        ReciteSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new cn.bugstack.recite.domain.recite.exception.ReciteException(
                        cn.bugstack.recite.types.enums.ResponseCode.SESSION_NOT_FOUND.getCode(),
                        "背诵会话不存在或已过期"));
        gateService.validateSession(session, userId);

        List<ReciteRecordEntity> records = recordPort.findBySessionId(userId, sessionId);

        double total = records.stream().filter(r -> r.getScore() != null)
                .mapToInt(ReciteRecordEntity::getScore).sum();
        double avg = records.stream().filter(r -> r.getScore() != null)
                .mapToInt(ReciteRecordEntity::getScore).average().orElse(0);
        int count = records.size();

        var moduleScores = records.stream()
                .filter(r -> r.getScore() != null)
                .collect(java.util.stream.Collectors.groupingBy(ReciteRecordEntity::getModuleKey,
                        java.util.stream.Collectors.averagingInt(ReciteRecordEntity::getScore)));
        List<String> strengths = moduleScores.entrySet().stream()
                .filter(e -> e.getValue() >= 7).map(java.util.Map.Entry::getKey).toList();
        List<String> weaknesses = moduleScores.entrySet().stream()
                .filter(e -> e.getValue() <= 4).map(java.util.Map.Entry::getKey).toList();

        session.setStatus("FINISHED");
        sessionPort.update(session);
        streakService.checkIn(userId);

        log.info("结束背诵: userId={}, sid={}, 均分={}", userId, sessionId, avg);

        List<Long> recordIds = records.stream().map(ReciteRecordEntity::getId).toList();
        reportMessagePort.sendReportRequest(userId, sessionId, recordIds);
        achievementMessagePort.sendAchievementRequest(userId, sessionId);

        knowledgeService.clearMemory(sessionId);

        return new SessionReportVO(total, avg, count, strengths, weaknesses, "报告生成中，请稍后查看");
    }

    /** 执行工具调用（评分槽外），最多 3 个 */
    private List<SkillResultVO> executeSkills(
            List<LlmPort.ToolCallRequest> toolCalls, QuestionEntity question, String answer) {
        List<SkillResultVO> results = new ArrayList<>();
        int limit = Math.min(toolCalls.size(), 3);
        for (int i = 0; i < limit; i++) {
            LlmPort.ToolCallRequest tc = toolCalls.get(i);
            boolean acquired = skillSlotPort.tryAcquire(15_000);
            if (!acquired) {
                log.warn("Skill 槽获取超时: {}", tc.functionName());
                results.add(SkillResultVO.error(tc.functionName(), tc.functionName(), "Skill 执行排队超时"));
                continue;
            }
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("question", question.getQuestion());
                params.put("answer", answer);
                params.put("referenceAnswer", question.getContent());
                results.add(skillPort.execute(tc.functionName(), params));
            } catch (Exception e) {
                log.warn("Skill 执行异常 [{}]: {}", tc.functionName(), e.getMessage());
                results.add(SkillResultVO.error(tc.functionName(), tc.functionName(), e.getMessage()));
            } finally {
                skillSlotPort.release();
            }
        }
        return results;
    }

    /** REVIEW 自评映射：想起→9, 不确定→5, 忘了→2 */
    private int mapSelfAssessment(String answer) {
        if (answer == null) return 5;
        String a = answer.trim();
        if (a.contains("想起")) return 9;
        if (a.contains("忘")) return 2;
        return 5;
    }
}
