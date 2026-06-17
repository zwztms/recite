package cn.bugstack.recite.domain.recite.model.valueobj;

import java.util.List;

/**
 * LLM 评分返回值对象.
 */
public record ScoreResultVO(
        /** 1-10 */
        Integer score,
        /** 正确点 */
        List<String> correctPoints,
        /** 遗漏点 */
        List<String> missedPoints,
        /** 改进建议 */
        String suggestion,
        /** 追问题目（可空） */
        String followUpQuestion) {
}
