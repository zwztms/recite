package cn.bugstack.recite.infrastructure.adapter.llm;

import cn.bugstack.recite.domain.knowledge.port.out.QueryRewriterPort;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * DeepSeek 驱动的查询改写.
 * 5 条改写规则: 口语→术语、补全省略、多概念拆分、只输出 JSON、保守策略.
 * LLM 失败后自动降级为规则提取（英文标识符 + 原题）.
 */
@Slf4j
@Service
public class DeepSeekQueryRewriter implements QueryRewriterPort {

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final String apiKey;
    private final String chatUrl;

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]{2,}");

    public DeepSeekQueryRewriter(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.chat-url:https://api.deepseek.com/v1/chat/completions}") String chatUrl) {
        this.apiKey = apiKey;
        this.chatUrl = chatUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).build();
    }

    @Override
    public RewriteResult rewrite(String userAnswer, String questionTitle, String moduleKey) {
        String prompt = String.format("""
                你是技术术语标准化助手。用户正在背诵"%s"模块的八股文。
                请将用户的口语化回答改写为精确的技术检索查询。

                规则：
                1. 将口语表达替换为标准技术术语（如"双指针滑动"→"滑动窗口"）
                2. 补全省略的主语和宾语
                3. 如果答案涉及多个独立概念，拆分为 2-3 个子查询
                4. 不确定的术语不强行改，保持原词
                5. 只输出 JSON，不要 markdown 包裹

                原题：%s
                用户回答：%s

                输出格式：{"rewritten":"改写后的主查询","subQueries":["子查询1","子查询2"]}
                """, moduleKey, questionTitle, userAnswer);

        try {
            String content = callLLM(prompt);
            return parseResult(content);
        } catch (Exception e) {
            log.warn("QueryRewriter LLM 失败, 降级为规则兜底: {}", e.getMessage());
            return fallback(userAnswer, questionTitle);
        }
    }

    private RewriteResult parseResult(String raw) {
        try {
            String s = raw.trim();
            if (s.startsWith("```")) s = s.replaceAll("```[a-z]*\\s*", "").replaceAll("```", "").trim();
            JsonObject obj = gson.fromJson(s, JsonObject.class);
            String rewritten = obj.has("rewritten") ? obj.get("rewritten").getAsString() : "";
            List<String> subs = new ArrayList<>();
            if (obj.has("subQueries")) {
                JsonArray arr = obj.getAsJsonArray("subQueries");
                arr.forEach(e -> subs.add(e.getAsString()));
            }
            return new RewriteResult(rewritten, subs);
        } catch (Exception e) {
            log.warn("QueryRewriter JSON 解析失败: {}", raw);
            return new RewriteResult("", List.of());
        }
    }

    /** 规则兜底: 提取英文标识符 + 原题作为主 query */
    private RewriteResult fallback(String answer, String question) {
        List<String> terms = new ArrayList<>();
        java.util.regex.Matcher m = WORD_PATTERN.matcher(answer);
        while (m.find()) terms.add(m.group());
        return new RewriteResult(question, terms);
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
}
