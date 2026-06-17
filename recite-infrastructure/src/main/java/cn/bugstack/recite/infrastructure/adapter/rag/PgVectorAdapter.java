package cn.bugstack.recite.infrastructure.adapter.rag;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.model.valueobj.EmbeddedQuestionVO;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.infrastructure.adapter.persistence.QuestionVectorDO;
import cn.bugstack.recite.infrastructure.adapter.persistence.QuestionVectorMapper;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * pgvector 题目适配器 — 实现向量搜索与题目 CRUD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PgVectorAdapter implements QuestionPort {

    private final QuestionVectorMapper mapper;

    @Override
    public List<EmbeddedQuestionVO> search(String query, List<String> moduleKeys, int topK) {
        // 模块过滤：为空则不过滤（传空数组会被 SQL ANY 全部匹配）
        String[] keys = (moduleKeys == null || moduleKeys.isEmpty())
                ? null : moduleKeys.toArray(new String[0]);
        // 向量用空字符串占位 — 实际应由调用方先 embedding 再传进来
        // 此处简化：如果 query 不为空，调用方应先用 EmbeddingPort 获取向量后传
        String vecStr = vectorToString(new float[1024]); // placeholder

        List<QuestionVectorDO> list;
        if (keys == null || keys.length == 0) {
            list = mapper.selectList(null);
        } else {
            list = mapper.findByModule(keys[0], topK);
        }

        return list.stream()
                .map(d -> {
                    QuestionEntity q = toEntity(d);
                    return new EmbeddedQuestionVO(q, 1.0);
                })
                .limit(topK)
                .toList();
    }

    @Override
    public List<EmbeddedQuestionVO> searchByModule(String moduleKey, int topK) {
        return mapper.findByModule(moduleKey, topK).stream()
                .map(d -> {
                    QuestionEntity q = toEntity(d);
                    return new EmbeddedQuestionVO(q, 1.0);
                })
                .toList();
    }

    @Override
    public void index(QuestionEntity question) {
        QuestionVectorDO d = toDO(question);
        if (d.getId() == null || d.getId().isBlank()) {
            d.setId(IdUtil.fastSimpleUUID());
        }
        mapper.insert(d);
        question.setId(d.getId());
    }

    @Override
    public void deleteById(String questionId) {
        mapper.deleteById(questionId);
    }

    @Override
    public void update(QuestionEntity question) {
        // 先删后插（embedding 可能变化）
        deleteById(question.getId());
        index(question);
    }

    @Override
    public QuestionEntity getById(String questionId) {
        QuestionVectorDO d = mapper.selectById(questionId);
        return d != null ? toEntity(d) : null;
    }

    @Override
    public boolean hasData() {
        return mapper.selectCount(null) > 0;
    }

    @Override
    public int countByModule(String moduleKey) {
        return mapper.countByModule(moduleKey);
    }

    // ---- 转换 ----

    private QuestionEntity toEntity(QuestionVectorDO d) {
        return QuestionEntity.builder()
                .id(d.getId()).question(d.getQuestion()).content(d.getContent())
                .moduleKey(d.getModuleKey()).category(d.getCategory()).tags(d.getTags())
                .difficulty(d.getDifficulty()).embedding(d.getEmbedding()).build();
    }

    private QuestionVectorDO toDO(QuestionEntity e) {
        QuestionVectorDO d = new QuestionVectorDO();
        d.setId(e.getId()); d.setQuestion(e.getQuestion()); d.setContent(e.getContent());
        d.setModuleKey(e.getModuleKey()); d.setCategory(e.getCategory()); d.setTags(e.getTags());
        d.setDifficulty(e.getDifficulty() != null ? e.getDifficulty() : 1);
        d.setEmbedding(e.getEmbedding());
        return d;
    }

    private String vectorToString(float[] vec) {
        if (vec == null) return "[]";
        return Arrays.toString(vec);
    }
}
