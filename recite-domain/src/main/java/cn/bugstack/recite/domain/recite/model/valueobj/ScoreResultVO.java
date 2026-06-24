package cn.bugstack.recite.domain.recite.model.valueobj;

import cn.bugstack.recite.types.skill.SkillResultVO;

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
        String followUpQuestion,
        /** Phase 17: Skill 执行结果列表 */
        List<SkillResultVO> skillResults) {

    /** 兼容旧调用（无 skill） */
    public ScoreResultVO(Integer score, List<String> correctPoints,
                         List<String> missedPoints, String suggestion, String followUpQuestion) {
        this(score, correctPoints, missedPoints, suggestion, followUpQuestion, List.of());
    }
}
