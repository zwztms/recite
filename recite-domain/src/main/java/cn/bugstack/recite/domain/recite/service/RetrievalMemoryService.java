package cn.bugstack.recite.domain.recite.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检索上下文记忆 — 两级记忆（L1工作 + L2摘要压缩）.
 * 对标 Ragent ConversationMemoryService 滑动窗口模式.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalMemoryService {

    private final MemoryCompressor compressor;

    /** L1 工作记忆: sessionId → 最近 N 轮 Q&A */
    private final Map<String, LinkedList<MemoryEntry>> workingMemory = new ConcurrentHashMap<>();

    /** L2 摘要记忆: sessionId → LLM 压缩的历史摘要链 */
    private final Map<String, List<String>> summaryMemory = new ConcurrentHashMap<>();

    private static final int WINDOW_SIZE = 5;

    /** 记录一轮问答 */
    public void record(String sessionId, String question, String answer,
                        List<String> missedPoints, String moduleKey) {
        LinkedList<MemoryEntry> window = workingMemory
                .computeIfAbsent(sessionId, k -> new LinkedList<>());
        window.addLast(new MemoryEntry(question, answer, missedPoints, moduleKey));

        // 窗口满了 → 触发 L2 压缩
        while (window.size() > WINDOW_SIZE) {
            MemoryEntry oldest = window.removeFirst();
            try {
                String summary = compressor.compress(oldest);
                summaryMemory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(summary);
            } catch (Exception e) {
                log.warn("记忆压缩失败: {}", e.getMessage());
            }
        }
    }

    /** 构建检索上下文 queries */
    public List<String> buildContextQueries(String sessionId, String currentQuestion) {
        List<String> queries = new ArrayList<>();
        queries.add(currentQuestion);

        // L1: 最近 N 轮的遗漏点
        LinkedList<MemoryEntry> window = workingMemory.get(sessionId);
        if (window != null) {
            Set<String> missedTerms = new LinkedHashSet<>();
            for (MemoryEntry entry : window) {
                if (entry.missedPoints != null) missedTerms.addAll(entry.missedPoints);
            }
            if (!missedTerms.isEmpty()) {
                queries.add(String.join(" ", missedTerms));
            }
        }

        // L2: 历史摘要
        List<String> summaries = summaryMemory.get(sessionId);
        if (summaries != null && !summaries.isEmpty()) {
            // 取最近 2 条摘要作为补充 query
            int start = Math.max(0, summaries.size() - 2);
            for (int i = start; i < summaries.size(); i++) {
                queries.add(summaries.get(i));
            }
        }

        return queries;
    }

    /** 获取最近 N 轮的 missedPoints（供 IntentRouter 使用） */
    public List<String> getRecentMissedPoints(String sessionId) {
        LinkedList<MemoryEntry> window = workingMemory.get(sessionId);
        if (window == null) return List.of();
        return window.stream()
                .filter(e -> e.missedPoints != null)
                .flatMap(e -> e.missedPoints.stream()).toList();
    }

    /** 会话结束后清理 */
    public void clear(String sessionId) {
        workingMemory.remove(sessionId);
        summaryMemory.remove(sessionId);
    }

    private record MemoryEntry(String question, String answer,
                               List<String> missedPoints, String moduleKey) {}
}
