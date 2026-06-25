package cn.bugstack.recite.infrastructure.adapter.llm;

import cn.bugstack.recite.domain.knowledge.port.out.RAGEvaluatorPort;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * DeepSeek 驱动的 RAGAS 四指标评估.
 * 每个指标由 LLM 独立打分 (1-10)，取平均换算为 0-1 分数.
 */
@Slf4j
@Service
public class DeepSeekRAGEvaluator implements RAGEvaluatorPort {

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final String apiKey;
    private final String chatUrl;

    public DeepSeekRAGEvaluator(OkHttpClient client,
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.chat-url:https://api.deepseek.com/v1/chat/completions}") String chatUrl) {
        this.client = client;
        this.apiKey = apiKey;
        this.chatUrl = chatUrl;
    }

    @Override
    public RAGEvaluationResult evaluate(String sessionId, String question, String answer,
                                         String aiSuggestion, List<String> retrievedChunks) {
        String chunksText = String.join("\n---\n", retrievedChunks);

        String prompt = String.format("""
                你是 RAG 系统的质量评估器。请根据以下信息，对四个维度分别打分(1-10)。

                【原题】%s
                【用户回答】%s
                【AI 评分建议】%s
                【检索到的知识片段】(共%d条)
                %s

                四个维度：
                1. Faithfulness(忠实度): AI建议中的论断是否可追溯到检索片段？有没有幻觉？
                2. ContextRelevance(上下文相关度): 检索片段中有多少真正与用户回答相关？
                3. ContextRecall(上下文召回率): AI建议中引用的知识点是否都在检索结果中？
                4. AnswerRelevance(答案相关度): AI建议整体是否与用户回答+原题相关？

                严格返回 JSON: {"faithfulness":8,"contextRelevance":7,"contextRecall":6,"answerRelevance":8}
                不要 markdown 包裹。
                """, question, answer, aiSuggestion, retrievedChunks.size(), chunksText);

        try {
            String content = callLLM(prompt);
            JsonObject obj = gson.fromJson(content.trim(), JsonObject.class);
            return new RAGEvaluationResult(
                    obj.get("faithfulness").getAsDouble() / 10.0,
                    obj.get("contextRelevance").getAsDouble() / 10.0,
                    obj.get("contextRecall").getAsDouble() / 10.0,
                    obj.get("answerRelevance").getAsDouble() / 10.0);
        } catch (Exception e) {
            log.warn("RAGEvaluator 失败(降级为0): {}", e.getMessage());
            return new RAGEvaluationResult(0, 0, 0, 0);
        }
    }

    private String callLLM(String prompt) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", "deepseek-chat");
        body.addProperty("temperature", 0.0);
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
