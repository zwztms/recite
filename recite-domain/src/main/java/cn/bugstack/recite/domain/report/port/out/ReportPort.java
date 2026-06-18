package cn.bugstack.recite.domain.report.port.out;

import cn.bugstack.recite.domain.report.model.entity.LearningJournal;

import java.util.List;

/**
 * 学习档案持久化 SPI.
 */
public interface ReportPort {

    /** 新增学习档案 */
    void save(LearningJournal journal);

    /** 按会话 ID 查报告（前端轮询用） */
    LearningJournal findBySessionId(String sessionId);

    /** 按 ID 查详情 */
    LearningJournal findById(Long id);

    /** 最近 N 次档案（注入 LLM 上下文） */
    List<LearningJournal> findRecentJournals(Long userId, int limit);

    /** 分页查档案列表 */
    List<LearningJournal> findJournals(Long userId, int page, int size);

    /** 用户累计报告次数 */
    int countByUserId(Long userId);
}
