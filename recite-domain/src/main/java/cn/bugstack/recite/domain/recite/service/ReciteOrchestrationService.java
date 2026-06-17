package cn.bugstack.recite.domain.recite.service;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.model.valueobj.EmbeddedQuestionVO;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.model.entity.ReciteSession;
import cn.bugstack.recite.domain.recite.model.valueobj.ScoreResultVO;
import cn.bugstack.recite.domain.recite.model.valueobj.SessionReportVO;
import cn.bugstack.recite.domain.recite.port.out.LlmPort;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.domain.recite.port.out.ReciteSessionPort;
import cn.bugstack.recite.domain.recite.port.out.ScoreSlotPort;
import cn.bugstack.recite.types.enums.ReciteMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 背诵核心编排器 — 不调外部 API，全部通过 Port 接口编排.
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

    public ReciteOrchestrationService(QuestionPort questionPort,
                                       ReciteSessionPort sessionPort,
                                       ScoreSlotPort scoreSlotPort,
                                       LlmPort llmPort,
                                       ReciteRecordPort recordPort,
                                       ReciteGateService gateService) {
        this.questionPort = questionPort;
        this.sessionPort = sessionPort;
        this.scoreSlotPort = scoreSlotPort;
        this.llmPort = llmPort;
        this.recordPort = recordPort;
        this.gateService = gateService;
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
                vos = questionPort.searchByModule(key, count);
            }
            case RANDOM -> vos = questionPort.search("", moduleKeys, count);
            case REVIEW -> throw new UnsupportedOperationException("REVIEW 模式待 Phase 6 progress 实现");
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

    /** 提交答案 → 校验 + 抢槽 + LLM 评分 + 释放 + 存记录 + 更新会话 */
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

        // 3. 抢评分槽位
        boolean acquired = scoreSlotPort.tryAcquire(30_000);
        if (!acquired) {
            throw new cn.bugstack.recite.domain.recite.exception.ReciteException(
                    cn.bugstack.recite.types.enums.ResponseCode.LLM_TIMEOUT.getCode(),
                    "评分排队超时，请稍后重试");
        }

        ScoreResultVO vo;
        try {
            // 4. LLM 评分
            vo = llmPort.score(question, answer);
            gateService.validateScore(vo.score());
        } finally {
            // 5. 释放槽位
            scoreSlotPort.release();
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

        // 7. 更新会话
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
        // 1. 校验会话
        ReciteSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new cn.bugstack.recite.domain.recite.exception.ReciteException(
                        cn.bugstack.recite.types.enums.ResponseCode.SESSION_NOT_FOUND.getCode(),
                        "背诵会话不存在或已过期"));
        gateService.validateSession(session, userId);
        gateService.validateFollowUpDepth(session.getFollowUpDepth());

        // 2. 查原记录 + 题目
        ReciteRecordEntity record = recordPort.findById(recordId);
        if (record == null) {
            throw new cn.bugstack.recite.domain.recite.exception.ReciteException("404", "记录不存在");
        }
        QuestionEntity question = questionPort.getById(record.getQuestionId());

        // 3. LLM 追问反馈
        String feedback = llmPort.followUp(question, followUpAnswer);

        // 4. 更新记录
        recordPort.updateFollowUp(recordId, followUpAnswer, feedback);

        // 5. 递增追问深度
        session.setFollowUpDepth(session.getFollowUpDepth() + 1);
        sessionPort.update(session);

        return feedback;
    }

    /** 结束背诵 → 统计 + LLM 报告 + 标记完成 */
    public SessionReportVO finishRecite(Long userId, String sessionId) {
        // 1. 校验
        ReciteSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new cn.bugstack.recite.domain.recite.exception.ReciteException(
                        cn.bugstack.recite.types.enums.ResponseCode.SESSION_NOT_FOUND.getCode(),
                        "背诵会话不存在或已过期"));
        gateService.validateSession(session, userId);

        // 2. 查全部记录
        List<ReciteRecordEntity> records = recordPort.findBySessionId(userId, sessionId);

        // 3. LLM 生成报告（内含 Java 统计）
        SessionReportVO report = llmPort.generateReport(records);

        // 4. 标记完成
        session.setStatus("FINISHED");
        sessionPort.update(session);
        log.info("结束背诵: userId={}, sid={}, 均分={}", userId, sessionId, report.averageScore());

        return report;
    }
}
