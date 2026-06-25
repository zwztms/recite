package cn.bugstack.recite.infrastructure.adapter.skill;

import cn.bugstack.recite.domain.recite.port.out.SkillPort;
import cn.bugstack.recite.domain.recite.service.SkillRegistry;
import cn.bugstack.recite.types.skill.SkillDefinition;
import cn.bugstack.recite.types.skill.SkillResultVO;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * 实现 SkillPort — 子 LLM 调用.
 * system=skill.body, user=题目+回答，返回结构化 JSON.
 */
@Slf4j
@Service
public class DeepSeekSkillAdapter implements SkillPort {

    private final SkillRegistry registry;
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final String apiKey;
    private final String chatUrl;

    public DeepSeekSkillAdapter(OkHttpClient client, SkillRegistry registry,
                                @Value("${deepseek.api-key}") String apiKey,
                                @Value("${deepseek.chat-url:https://api.deepseek.com/v1/chat/completions}") String chatUrl) {
        this.client = client;
        this.registry = registry;
        this.apiKey = apiKey;
        this.chatUrl = chatUrl;
    }

    @Override
    public List<ToolDefinition> listToolDefinitions() {
        return registry.buildToolDefinitions();
    }

    @Override
    public SkillResultVO execute(String skillName, Map<String, Object> params) {
        SkillDefinition skill = registry.get(skillName);
        if (skill == null) {
            return SkillResultVO.error(skillName, skillName, "Skill 未注册");
        }
        String question = (String) params.getOrDefault("question", "");
        String answer = (String) params.getOrDefault("answer", "");
        String userMsg = String.format(
                "题目：%s\n用户回答：%s\n请根据你的职责进行分析，严格返回 JSON。", question, answer);
        try {
            String raw = callApi(skill.body(), userMsg);
            String json = extractJson(raw);
            return new SkillResultVO(skillName, skill.label(), json, parseMap(json));
        } catch (Exception e) {
            log.error("Skill [{}] 执行失败: {}", skillName, e.getMessage());
            return SkillResultVO.error(skillName, skill.label(), "Skill 执行超时");
        }
    }

    private String callApi(String systemPrompt, String userMessage) {
        JsonObject body = new JsonObject();
        body.addProperty("model", "deepseek-chat");
        body.addProperty("temperature", 0.3);
        JsonArray msgs = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userMessage);
        msgs.add(sys);
        msgs.add(usr);
        body.add("messages", msgs);
        try {
            Request req = new Request.Builder().url(chatUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response resp = client.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.error("Skill API error {}", resp.code());
                    return "{}";
                }
                String r = resp.body() != null ? resp.body().string() : "";
                return gson.fromJson(r, JsonObject.class)
                        .getAsJsonArray("choices").get(0).getAsJsonObject()
                        .getAsJsonObject("message").get("content").getAsString();
            }
        } catch (IOException e) {
            log.error("Skill API 失败: {}", e.getMessage());
            return "{}";
        }
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isEmpty()) return "{}";
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("```[a-z]*\\s*", "").replaceAll("```", "").trim();
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        try {
            return gson.fromJson(json, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
