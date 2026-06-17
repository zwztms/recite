package cn.bugstack.recite.domain.knowledge.port.out;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.model.valueobj.EmbeddedQuestionVO;

import java.util.List;

/**
 * 题目查询与向量搜索 SPI — infra 层实现（pgvector）.
 *
 * <p>这是 recite 子域出题的唯一入口，后续 Phase 5 依赖此接口.</p>
 */
public interface QuestionPort {

    // ==== 语义搜索 ====

    /**
     * 语义搜索 — 面向背诵的核心出题方法.
     *
     * @param query      搜索文本（为空时用默认词）
     * @param moduleKeys 限定模块范围（为空则不限）
     * @param topK       返回数量上限
     */
    List<EmbeddedQuestionVO> search(String query, List<String> moduleKeys, int topK);

    /** 按模块列出（管理后台用） */
    List<EmbeddedQuestionVO> searchByModule(String moduleKey, int topK);

    // ==== 题目管理 ====

    /** 新增/重建索引（调用方已填充 embedding） */
    void index(QuestionEntity question);

    /** 删除题目 */
    void deleteById(String questionId);

    /** 更新题目（先删后插，触发 embedding 重新计算） */
    void update(QuestionEntity question);

    /** 按 ID 查单题 */
    QuestionEntity getById(String questionId);

    /** 题库是否有数据 */
    boolean hasData();

    // ==== 计数查询（Phase 7/8 复用） ====

    /** 某模块题目数 */
    int countByModule(String moduleKey);
}
