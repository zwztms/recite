package cn.bugstack.recite.infrastructure.adapter.rag;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.model.valueobj.EmbeddedQuestionVO;
import cn.bugstack.recite.domain.knowledge.port.out.EmbeddingPort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.infrastructure.adapter.persistence.QuestionVectorDO;
import cn.bugstack.recite.infrastructure.adapter.persistence.QuestionVectorMapper;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * pgvector 题目适配器 — 实现向量搜索与题目 CRUD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PgVectorAdapter implements QuestionPort {

    private final QuestionVectorMapper mapper;
    private final EmbeddingPort embeddingPort;

    @Override
    public List<EmbeddedQuestionVO> search(String query, List<String> moduleKeys, int topK) {
        String[] keys = (moduleKeys == null || moduleKeys.isEmpty())
                ? null : moduleKeys.toArray(new String[0]);

        // 文本转向量 → pgvector 余弦搜索
        float[] queryVector = embeddingPort.embed(query != null ? query : "");
        if (queryVector == null || queryVector.length == 0) {
            log.warn("Embedding 返回空向量，降级为按模块查询");
            return searchByModule(null, topK);
        }

        String vecStr = vectorToPgString(queryVector);
        List<QuestionVectorDO> list;
        if (keys == null || keys.length == 0) {
            list = mapper.searchByVectorAll(vecStr, topK);
        } else {
            list = mapper.searchByVector(vecStr, keys, topK);
        }

        return list.stream()
                .map(d -> {
                    QuestionEntity q = toEntity(d);
                    return new EmbeddedQuestionVO(q, d.getSimilarity());
                })
                .toList();
    }

    @Override
    public List<EmbeddedQuestionVO> searchRandom(List<String> moduleKeys, int topK) {
        List<QuestionVectorDO> all;
        if (moduleKeys == null || moduleKeys.isEmpty()) {
            all = mapper.selectList(null);
        } else {
            all = new java.util.ArrayList<>();
            for (String key : moduleKeys) {
                all.addAll(mapper.findByModule(key, 10000));
            }
        }
        java.util.Collections.shuffle(all);
        return all.stream()
                .limit(topK)
                .map(d -> new EmbeddedQuestionVO(toEntity(d), 1.0))
                .toList();
    }

    @Override
    public List<EmbeddedQuestionVO> searchByModule(String moduleKey, int topK) {
        List<QuestionVectorDO> list;
        if (moduleKey == null) {
            list = mapper.selectList(null);
        } else {
            list = mapper.findByModule(moduleKey, topK);
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

    /** float[] → pgvector 兼容字符串 "[0.1,0.2,...]" */
    static String vectorToPgString(float[] vec) {
        if (vec == null || vec.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        return sb.append("]").toString();
    }
}
