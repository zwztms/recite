package cn.bugstack.recite.infrastructure.adapter.embedding;

import cn.bugstack.recite.domain.knowledge.port.out.EmbeddingPort;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SiliconFlow Embedding 适配器 — Qwen3-Embedding-0.6B, 1024 维.
 */
@Slf4j
@Service
public class SiliconFlowEmbeddingAdapter implements EmbeddingPort {

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    private final String apiKey;
    private final String embeddingUrl;

    public SiliconFlowEmbeddingAdapter(
            @Value("${siliconflow.api-key}") String apiKey,
            @Value("${siliconflow.embedding-url:https://api.siliconflow.cn/v1/embeddings}") String embeddingUrl) {
        this.apiKey = apiKey;
        this.embeddingUrl = embeddingUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", "Qwen/Qwen3-Embedding-0.6B");
            JsonArray input = new JsonArray();
            texts.forEach(input::add);
            body.add("input", input);

            Request request = new Request.Builder()
                    .url(embeddingUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    log.error("SiliconFlow API error {}: {}", response.code(), errBody);
                    return List.of();
                }

                String respBody = response.body() != null ? response.body().string() : "";
                JsonObject root = gson.fromJson(respBody, JsonObject.class);
                JsonArray data = root.getAsJsonArray("data");
                List<float[]> embeddings = new ArrayList<>();

                for (int i = 0; i < data.size(); i++) {
                    JsonArray vecArray = data.get(i).getAsJsonObject().getAsJsonArray("embedding");
                    float[] vec = new float[vecArray.size()];
                    for (int j = 0; j < vecArray.size(); j++) {
                        vec[j] = vecArray.get(j).getAsFloat();
                    }
                    embeddings.add(vec);
                }
                return embeddings;
            }
        } catch (IOException e) {
            log.error("SiliconFlow embedding failed: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
