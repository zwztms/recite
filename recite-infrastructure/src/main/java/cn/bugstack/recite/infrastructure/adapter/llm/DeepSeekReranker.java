package cn.bugstack.recite.infrastructure.adapter.llm;

import cn.bugstack.recite.domain.knowledge.port.out.RerankerPort;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * LLM Cross-Encoder 重排器.
 * query + 每个候选 chunk → LLM 打分 1-10 → 过滤 score≥5 → 排序取 top-K.
 * 失败降级为原序截取.
 */
@Slf4j
@Service
public class DeepSeekReranker implements RerankerPort {

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final String apiKey;
    private final String chatUrl;

    public DeepSeekReranker(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.chat-url:https://api.deepseek.com/v1/chat/completions}") String chatUrl) {
        this.apiKey = apiKey;
        this.chatUrl = chatUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build();
    }

    @Override
    public List<String> rerank(String query, List<String> candidates, int topK) {
        if (candidates == null || candidates.size() <= topK) return candidates;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("[").append(i).append("] ").append(candidates.get(i)).append("\n\n");
        }

        String prompt = String.format("""
                你是检索相关度评估器。用户正在背诵技术八股文，
                以下是检索到的一些知识片段。请评估每个片段与用户查询的相关度(1-10分)。

                【用户查询】%s

                【候选片段】
                %s

                严格返回 JSON 数组: [{"index":0,"score":8,"reason":"..."}]
                只保留 score>=5 的片段，不要 markdown 包裹。
                """, query, sb.toString());

        try {
            String content = callLLM(prompt);
            return parseScores(content, candidates, topK);
        } catch (Exception e) {
            log.warn("Reranker 失败(降级为原序): {}", e.getMessage());
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseScores(String raw, List<String> candidates, int topK) {
        try {
            String s = raw.trim();
            if (s.startsWith("```"))
                s = s.replaceAll("```[a-z]*\\s*", "").replaceAll("```", "").trim();
            List<Map<String, Object>> scores = gson.fromJson(s, List.class);
            return scores.stream()
                    .filter(m -> ((Number) m.get("score")).intValue() >= 5)
                    .sorted((a, b) -> Integer.compare(
                            ((Number) b.get("score")).intValue(),
                            ((Number) a.get("score")).intValue()))
                    .limit(topK)
                    .map(m -> {
                        int idx = ((Number) m.get("index")).intValue();
                        return idx < candidates.size() ? candidates.get(idx) : "";
                    }).filter(t -> !t.isEmpty()).toList();
        } catch (Exception e) {
            log.warn("Reranker JSON 解析失败: {}", raw);
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }
    }

    private String callLLM(String prompt) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", "deepseek-chat");
        body.addProperty("temperature", 0.1);
        JsonArray msgs = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        msgs.add(msg);
        body.add("messages", msgs);

        Request req = new Request.Builder().url(chatUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            String r = resp.body() != null ? resp.body().string() : "";
            return gson.fromJson(r, JsonObject.class).getAsJsonArray("choices")
                    .get(0).getAsJsonObject().getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }
}
