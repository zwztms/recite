package cn.bugstack.recite.infrastructure.adapter.persistence;

import cn.bugstack.recite.domain.report.model.entity.LearningJournal;
import cn.bugstack.recite.domain.report.port.out.ReportPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学习档案持久化适配器 — MyBatis Plus 实现.
 */
@Repository
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements ReportPort {

    private final LearningJournalMapper mapper;

    @Override
    public void save(LearningJournal journal) {
        mapper.insert(toDO(journal));
    }

    @Override
    public LearningJournal findBySessionId(String sessionId) {
        LearningJournalDO d = mapper.selectOne(new LambdaQueryWrapper<LearningJournalDO>()
                .eq(LearningJournalDO::getSessionId, sessionId));
        return toEntity(d);
    }

    @Override
    public LearningJournal findById(Long id) {
        return toEntity(mapper.selectById(id));
    }

    @Override
    public List<LearningJournal> findRecentJournals(Long userId, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<LearningJournalDO>()
                        .eq(LearningJournalDO::getUserId, userId)
                        .orderByDesc(LearningJournalDO::getCreatedAt)
                        .last("LIMIT " + limit))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<LearningJournal> findJournals(Long userId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        return mapper.selectList(new LambdaQueryWrapper<LearningJournalDO>()
                        .eq(LearningJournalDO::getUserId, userId)
                        .orderByDesc(LearningJournalDO::getCreatedAt)
                        .last("LIMIT " + size + " OFFSET " + offset))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public int countByUserId(Long userId) {
        return Math.toIntExact(mapper.selectCount(new LambdaQueryWrapper<LearningJournalDO>()
                .eq(LearningJournalDO::getUserId, userId)));
    }

    // ---- 转换 ----

    private LearningJournal toEntity(LearningJournalDO d) {
        if (d == null) return null;
        return LearningJournal.builder()
                .id(d.getId()).userId(d.getUserId()).sessionId(d.getSessionId())
                .summaryJson(d.getSummaryJson()).createdAt(d.getCreatedAt()).build();
    }

    private LearningJournalDO toDO(LearningJournal e) {
        LearningJournalDO d = new LearningJournalDO();
        d.setId(e.getId()); d.setUserId(e.getUserId()); d.setSessionId(e.getSessionId());
        d.setSummaryJson(e.getSummaryJson()); d.setCreatedAt(e.getCreatedAt());
        return d;
    }
}
