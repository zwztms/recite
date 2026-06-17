package cn.bugstack.recite.domain.knowledge.model.valueobj;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;

/**
 * 搜索结果值对象 — 题目 + 相似度分数.
 */
public record EmbeddedQuestionVO(QuestionEntity question, Double similarityScore) {
}
