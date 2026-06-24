package cn.bugstack.recite.infrastructure.adapter.admin;

import cn.bugstack.recite.api.response.Response;
import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统设置控制器 — 只读展示当前系统配置.
 * <p>
 * 放在 infrastructure 模块，与 AdminMonitorController、UserController 同级.
 */
@RestController
@RequestMapping("/admin/settings")
@SaCheckRole("ADMIN")
public class SettingsController {

    @Value("${deepseek.api-key:}") private String dsKey;
    @Value("${deepseek.chat-url:https://api.deepseek.com/v1/chat/completions}") private String dsUrl;
    @Value("${siliconflow.api-key:}") private String sfKey;
    @Value("${siliconflow.embedding-url:}") private String sfUrl;

    @GetMapping
    public Response<Map<String, Object>> getSettings() {
        Map<String, Object> settings = Map.of(
            "llm", Map.of(
                "provider", "DeepSeek",
                "model", "deepseek-chat",
                "url", dsUrl,
                "keyMasked", maskKey(dsKey)),
            "embedding", Map.of(
                "provider", "SiliconFlow",
                "model", "Qwen3-Embedding-0.6B",
                "dimension", 1024,
                "url", sfUrl,
                "keyMasked", maskKey(sfKey)),
            "vectorStore", Map.of(
                "type", "pgvector",
                "metric", "COSINE",
                "indexType", "ivfflat(lists=100)"),
            "ragPipeline", Map.ofEntries(
                Map.entry("queryRewriter", true),
                Map.entry("intentRouter", true),
                Map.entry("multiChannel", true),
                Map.entry("hybridSearch", true),
                Map.entry("postProcess", true),
                Map.entry("mmrDiversifier", true),
                Map.entry("reranker", true),
                Map.entry("memory", true)),
            "concurrency", Map.of(
                "scoreSlot", 10,
                "type", "Redis RSemaphore"),
            "sse", Map.of(
                "timeout", "60s",
                "eventTypes", "score/correct/missed/skill/suggestion/followUp/done")
        );
        return Response.ok(settings);
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 12) return "***";
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }
}
