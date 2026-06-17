package cn.bugstack.recite.domain.recite.service;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
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

import java.util.List;

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
        // TODO Step 5
        throw new UnsupportedOperationException("startRecite 待实现");
    }

    /** 提交答案 → 校验 + 抢槽 + LLM 评分 + 释放 + 存记录 + 更新会话 */
    public ScoreResultVO submitAnswer(Long userId, String sessionId,
                                       String questionId, String answer) {
        // TODO Step 6
        throw new UnsupportedOperationException("submitAnswer 待实现");
    }

    /** 追问回答 → 校验深度 + LLM 追问 + 更新记录 */
    public String submitFollowUp(Long userId, String sessionId,
                                  Long recordId, String followUpAnswer) {
        // TODO Step 7
        throw new UnsupportedOperationException("submitFollowUp 待实现");
    }

    /** 结束背诵 → 统计 + LLM 报告 + MQ + 标记完成 */
    public SessionReportVO finishRecite(Long userId, String sessionId) {
        // TODO Step 8
        throw new UnsupportedOperationException("finishRecite 待实现");
    }
}
