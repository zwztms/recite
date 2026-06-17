package cn.bugstack.recite.domain.recite.port.out;

import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;

import java.util.List;

/**
 * recite_records 持久化 SPI.
 */
public interface ReciteRecordPort {

    /** 新增背诵记录 */
    ReciteRecordEntity save(ReciteRecordEntity record);

    /** 更新追问回答和反馈 */
    void updateFollowUp(Long recordId, String answer, String feedback);

    /** 查某场次全部记录 */
    List<ReciteRecordEntity> findBySessionId(Long userId, String sessionId);

    /** 查用户最近 N 条记录 */
    List<ReciteRecordEntity> findByUserId(Long userId, int limit);

    /** 按 ID 查 */
    ReciteRecordEntity findById(Long recordId);

    // ---- 统计（供 Phase 7/8 复用） ----

    int countByUserId(Long userId);
    double avgScoreByUserId(Long userId);
    int countByModule(Long userId, String moduleKey);
    int countPerfectScores(Long userId);
    int countSessionsByUserId(Long userId);
}
