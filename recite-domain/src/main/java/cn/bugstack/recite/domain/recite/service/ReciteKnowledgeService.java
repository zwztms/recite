package cn.bugstack.recite.domain.recite.service;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.port.out.*;
import cn.bugstack.recite.domain.knowledge.service.MultiChannelRetrievalEngine;
import cn.bugstack.recite.domain.knowledge.service.PostProcessingPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 背诵知识增强服务 — 封装 7 阶段 RAG 管线 + 降级 + 记忆 + 评估.
 * <p>
 * 从 ReciteOrchestrationService 提取，将 9 个 RAG 依赖封装为 4 个对外方法，
 * 使编排器构造参数从 22 降至 14.
 */
@Slf4j
@Service
public class ReciteKnowledgeService {

    private final QueryRewriterPort queryRewriter;
    private final KnowledgeRouterPort knowledgeRouter;
    private final MultiChannelRetrievalEngine retriever;
    private final PostProcessingPipeline postProcessor;
    private final RetrievalMemoryService retrievalMemory;
    private final RAGEvaluatorPort ragEvaluator;
    private final EvaluationRecordPort evalRecordPort;
    private final EmbeddingPort embeddingPort;
    private final KnowledgeChunkPort chunkPort;

    public ReciteKnowledgeService(QueryRewriterPort queryRewriter,
                                   KnowledgeRouterPort knowledgeRouter,
                                   MultiChannelRetrievalEngine retriever,
                                   PostProcessingPipeline postProcessor,
                                   RetrievalMemoryService retrievalMemory,
                                   RAGEvaluatorPort ragEvaluator,
                                   EvaluationRecordPort evalRecordPort,
                                   EmbeddingPort embeddingPort,
                                   KnowledgeChunkPort chunkPort) {
        this.queryRewriter = queryRewriter;
        this.knowledgeRouter = knowledgeRouter;
        this.retriever = retriever;
        this.postProcessor = postProcessor;
        this.retrievalMemory = retrievalMemory;
        this.ragEvaluator = ragEvaluator;
        this.evalRecordPort = evalRecordPort;
        this.embeddingPort = embeddingPort;
        this.chunkPort = chunkPort;
    }

    /**
     * 执行知识增强检索管线.
     * ①查询改写 → ②意图路由 → ③多通道检索 → ④后处理.
     * 任意阶段失败降级为纯向量检索，不影响背诵评分.
     *
     * @param question  当前题目
     * @param answer    用户回答
     * @param sessionId 背诵会话 ID
     * @return top-K 知识片段引用
     */
    public List<String> retrieve(QuestionEntity question, String answer, String sessionId) {
        try {
            // ① 查询改写
            QueryRewriterPort.RewriteResult rewrite = queryRewriter.rewrite(
                    answer, question.getQuestion(), question.getModuleKey());
            log.debug("RAG① 查询改写: {} → {} subQueries",
                    rewrite.rewrittenQuery(), rewrite.subQueries().size());

            // ② 意图路由
            KnowledgeRouterPort.RouteResult route = knowledgeRouter.route(
                    retrievalMemory.getRecentMissedPoints(sessionId),
                    question.getModuleKey(), 20);
            log.debug("RAG② 意图路由: {} allocations", route.allocations().size());

            // ③ 多通道检索
            float[] primaryEmb = embeddingPort.embed(rewrite.rewrittenQuery());
            List<String> candidates = retriever.retrieve(
                    rewrite.allQueries(), route, primaryEmb, 30);
            log.debug("RAG③ 多通道检索: {} candidates", candidates.size());

            // ④ 后处理管线（归一化→去重→MMR→Reranker）
            List<String> refs = postProcessor.process(candidates,
                    rewrite.rewrittenQuery(), primaryEmb, 5);
            log.debug("RAG④ 后处理: top-{} → {}", refs.size(), refs);
            return refs;

        } catch (Exception e) {
            log.warn("RAG管线失败(降级为纯向量检索, 评分不受影响): {}", e.getMessage());
            return fallbackRetrieve(question);
        }
    }

    /** 记录本轮弱点到上下文记忆 */
    public void recordMemory(String sessionId, String question, String answer,
                             List<String> missedPoints, String moduleKey) {
        try {
            retrievalMemory.record(sessionId, question, answer, missedPoints, moduleKey);
        } catch (Exception e) {
            log.warn("RAG记忆记录失败: {}", e.getMessage());
        }
    }

    /** 异步触发 RAGAS 四指标评估 */
    public void evaluateAsync(String sessionId, String question, String answer,
                               String suggestion, List<String> knowledgeRefs) {
        try {
            RAGEvaluatorPort.RAGEvaluationResult evalResult = ragEvaluator.evaluate(
                    sessionId, question, answer, suggestion, knowledgeRefs);
            evalRecordPort.save(sessionId, evalResult, knowledgeRefs, suggestion);
        } catch (Exception e) {
            log.warn("RAG评估失败(不影响背诵): {}", e.getMessage());
        }
    }

    /** 清理会话上下文记忆 */
    public void clearMemory(String sessionId) {
        try {
            retrievalMemory.clear(sessionId);
        } catch (Exception e) {
            // 静默清理
        }
    }

    /** 降级：纯向量检索 */
    private List<String> fallbackRetrieve(QuestionEntity question) {
        try {
            float[] emb = embeddingPort.embed(question.getQuestion());
            return chunkPort.searchSimilar(emb, 3);
        } catch (Exception e2) {
            log.warn("降级检索也失败: {}", e2.getMessage());
            return List.of();
        }
    }
}
