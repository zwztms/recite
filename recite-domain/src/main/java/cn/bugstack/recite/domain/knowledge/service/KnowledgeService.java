package cn.bugstack.recite.domain.knowledge.service;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.port.out.EmbeddingPort;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;

import java.util.List;

/**
 * 知识库领域服务 — 编排导入流程.
 *
 * <p>不调外部 API，只通过 Port 接口编排.</p>
 */
public class KnowledgeService {

    private final EmbeddingPort embeddingPort;
    private final QuestionPort questionPort;
    private final ModulePort modulePort;

    public KnowledgeService(EmbeddingPort embeddingPort, QuestionPort questionPort, ModulePort modulePort) {
        this.embeddingPort = embeddingPort;
        this.questionPort = questionPort;
        this.modulePort = modulePort;
    }

    /**
     * 从题目列表导入 — 批量 embedding → 逐题入库 → 更新模块计数.
     *
     * <p>文件 I/O 和解析由调用方（infra 层 FileImportAdapter）处理，
     * 此方法只负责 "拿到题目 → embedding → 入库" 的核心编排.</p>
     *
     * @param questions 已解析的题目列表（不含 embedding）
     * @return 成功导入数量
     */
    public int importQuestions(List<QuestionEntity> questions) {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }

        // 1. 批量获取向量
        List<String> contents = questions.stream()
                .map(q -> q.getContent() != null ? q.getContent() : "")
                .toList();
        List<float[]> embeddings = embeddingPort.embedBatch(contents);

        // 2. 逐题设置 embedding 并入库
        int count = 0;
        for (int i = 0; i < questions.size(); i++) {
            QuestionEntity q = questions.get(i);
            q.setEmbedding(embeddings.get(i));
            questionPort.index(q);
            count++;
        }

        // 3. 更新各模块题目计数
        questions.stream()
                .map(QuestionEntity::getModuleKey)
                .distinct()
                .forEach(key -> {
                    int cnt = questionPort.countByModule(key);
                    // 用已知的差值更新
                    long delta = questions.stream().filter(q -> key.equals(q.getModuleKey())).count();
                    modulePort.updateQuestionCount(key, (int) delta);
                });

        return count;
    }
}
