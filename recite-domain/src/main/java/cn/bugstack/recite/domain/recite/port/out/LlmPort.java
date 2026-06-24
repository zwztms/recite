package cn.bugstack.recite.domain.recite.port.out;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.model.valueobj.ScoreResultVO;
import cn.bugstack.recite.domain.recite.model.valueobj.SessionReportVO;

import java.util.List;
import java.util.Map;

/**
 * LLM 交互 SPI — DeepSeek 实现.
 */
public interface LlmPort {

    /** AI 评分 */
    ScoreResultVO score(QuestionEntity question, String userAnswer);

    /** AI 评分（含知识参考）—— Phase RAG 管线注入 */
    default ScoreResultVO score(QuestionEntity question, String userAnswer,
                                 List<String> knowledgeRefs) {
        return score(question, userAnswer);
    }

    /** 追问反馈 */
    String followUp(QuestionEntity question, String userAnswer);

    /** 生成报告 */
    SessionReportVO generateReport(List<ReciteRecordEntity> records);

    /** 生成学习档案报告（含历史上下文），返回结构化 JSON 存入 learning_journal */
    String generateJournalReport(List<ReciteRecordEntity> records, List<String> recentJournalSummaries);

    // ==== Phase 17: Skill 工具调用评分 ====

    /** AI 评分 + 可选 skill 调用（支持 tools 参数） */
    EnhancedScoreResult scoreWithSkills(QuestionEntity question, String userAnswer,
                                        List<Map<String, Object>> tools);

    record EnhancedScoreResult(
            int score, List<String> correctPoints, List<String> missedPoints,
            String suggestion, String followUpQuestion,
            List<ToolCallRequest> toolCalls, String rawResponse) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    record ToolCallRequest(String id, String functionName, String arguments) {}

    /** 记忆压缩 — 将旧窗口压缩为 ≤100 字薄弱点摘要 */
    String compress(String prompt);
}
