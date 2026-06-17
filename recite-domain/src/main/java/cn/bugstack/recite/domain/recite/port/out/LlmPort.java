package cn.bugstack.recite.domain.recite.port.out;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.model.valueobj.ScoreResultVO;
import cn.bugstack.recite.domain.recite.model.valueobj.SessionReportVO;

import java.util.List;

/**
 * LLM 交互 SPI — DeepSeek 实现.
 */
public interface LlmPort {

    /** AI 评分 */
    ScoreResultVO score(QuestionEntity question, String userAnswer);

    /** 追问反馈 */
    String followUp(QuestionEntity question, String userAnswer);

    /** 生成报告 */
    SessionReportVO generateReport(List<ReciteRecordEntity> records);
}
