package cn.bugstack.recite.infrastructure.adapter.llm;

import cn.bugstack.recite.domain.knowledge.port.out.ChunkEnricherPort;
import cn.bugstack.recite.domain.knowledge.port.out.ChunkerPort.Chunk;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek 驱动的 Chunk 上下文富化.
 * 为每个 chunk 生成一句话背景描述，提升 embedding 语义质量.
 */
@Slf4j
@Service
public class DeepSeekChunkEnricher implements ChunkEnricherPort {

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final String apiKey;
    private final String chatUrl;

    private static final int BATCH_SIZE = 10;

    public DeepSeekChunkEnricher(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.chat-url:https://api.deepseek.com/v1/chat/completions}") String chatUrl) {
        this.apiKey = apiKey;
        this.chatUrl = chatUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS).build();
    }

    @Override
    public List<EnrichedChunk> enrich(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return List.of();
        List<EnrichedChunk> results = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, chunks.size());
            List<Chunk> batch = chunks.subList(i, end);
            try {
                results.addAll(enrichBatch(batch));
            } catch (Exception e) {
                log.warn("Chunk富化失败(批次{}-{}), 降级为空摘要: {}", i, end, e.getMessage());
                for (Chunk c : batch) {
                    results.add(new EnrichedChunk(c.chunkText(), c.index(), "", c.docTitle()));
                }
            }
        }
        return results;
    }

    private List<EnrichedChunk> enrichBatch(List<Chunk> batch) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            sb.append("[").append(i).append("] ").append(batch.get(i).chunkText()).append("\n\n");
        }

        String prompt = String.format("""
                你是技术文档上下文分析助手。以下是文档"%s"的几个连续片段。
                请为每个片段写一句话(≤30字)的上下文背景描述，说明这个片段在原文中的位置和作用。

                %s

                严格返回 JSON 数组: [{"index":0,"summary":"..."},{"index":1,"summary":"..."}]
                不要 markdown 包裹。
                """, batch.get(0).docTitle(), sb.toString());

        String content = callLLM(prompt);
        List<Map<String, Object>> summaries = parseSummaries(content);

        List<EnrichedChunk> results = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            String summary = "";
            for (Map<String, Object> s : summaries) {
                if (((Number) s.get("index")).intValue() == i) {
                    summary = (String) s.get("summary");
                    break;
                }
            }
            Chunk c = batch.get(i);
            results.add(new EnrichedChunk(c.chunkText(), c.index(), summary, c.docTitle()));
        }
        return results;
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
            return gson.fromJson(r, JsonObject.class)
                    .getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseSummaries(String raw) {
        try {
            String s = raw.trim();
            if (s.startsWith("```")) s = s.replaceAll("```[a-z]*\\s*", "").replaceAll("```", "").trim();
            return gson.fromJson(s, List.class);
        } catch (Exception e) {
            log.warn("富化 JSON 解析失败: {}", raw);
            return List.of();
        }
    }
}
