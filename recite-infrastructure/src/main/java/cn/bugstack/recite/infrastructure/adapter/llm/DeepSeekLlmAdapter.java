package cn.bugstack.recite.infrastructure.adapter.llm;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.model.valueobj.ScoreResultVO;
import cn.bugstack.recite.domain.recite.model.valueobj.SessionReportVO;
import cn.bugstack.recite.domain.recite.port.out.LlmPort;
import cn.bugstack.recite.types.annotation.ReciteTraceNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DeepSeek LLM 适配器 — 评分 / 追问 / 报告.
 */
@Slf4j
@Service
public class DeepSeekLlmAdapter implements LlmPort {

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    private final String apiKey;
    private final String chatUrl;

    public DeepSeekLlmAdapter(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.chat-url:https://api.deepseek.com/v1/chat/completions}") String chatUrl) {
        this.apiKey = apiKey;
        this.chatUrl = chatUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @ReciteTraceNode(type = "LLM", name = "DeepSeek评分")
    @Override
    public ScoreResultVO score(QuestionEntity question, String userAnswer) {
        String prompt = """
                你是大厂校招面试官。根据题目和回答评分（1-10分）。

                题目：%s
                参考答案：%s
                用户回答：%s

                请严格返回以下JSON格式（不要markdown包裹）：
                {"score":8,"correctPoints":["点1","点2"],"missedPoints":["遗漏点"],"suggestion":"改进建议","followUpQuestion":"追问（可空字符串）"}
                """.formatted(question.getQuestion(), question.getContent(), userAnswer);

        String raw = callApi(prompt);
        String json = extractJson(raw);
        try {
            var map = gson.<Map<String, Object>>fromJson(json,
                    new TypeToken<Map<String, Object>>() {}.getType());
            int score = ((Number) map.get("score")).intValue();
            @SuppressWarnings("unchecked")
            List<String> correct = ((List<String>) map.getOrDefault("correctPoints", List.of()));
            @SuppressWarnings("unchecked")
            List<String> missed = ((List<String>) map.getOrDefault("missedPoints", List.of()));
            String suggestion = (String) map.getOrDefault("suggestion", "");
            String followUp = (String) map.getOrDefault("followUpQuestion", "");
            return new ScoreResultVO(score, correct, missed, suggestion, followUp);
        } catch (Exception e) {
            log.error("解析 LLM 评分结果失败: {}", json, e);
            return new ScoreResultVO(5, List.of(), List.of("解析异常"), "请重试", "");
        }
    }

    @Override
    public String followUp(QuestionEntity question, String userAnswer) {
        String prompt = """
                你是大厂校招面试官。根据用户对追问的回答，给出简短反馈（50字以内）。

                原题目：%s
                用户追问回答：%s
                """.formatted(question.getQuestion(), userAnswer);

        return callApi(prompt);
    }

    @Override
    public SessionReportVO generateReport(List<ReciteRecordEntity> records) {
        if (records == null || records.isEmpty()) {
            return new SessionReportVO(0.0, 0.0, 0, List.of(), List.of(), "无背诵记录");
        }

        double total = records.stream().filter(r -> r.getScore() != null)
                .mapToInt(ReciteRecordEntity::getScore).sum();
        double avg = records.stream().filter(r -> r.getScore() != null)
                .mapToInt(ReciteRecordEntity::getScore).average().orElse(0);
        int count = records.size();

        // 按模块分组统计均分
        var moduleScores = records.stream()
                .filter(r -> r.getScore() != null)
                .collect(Collectors.groupingBy(ReciteRecordEntity::getModuleKey,
                        Collectors.averagingInt(ReciteRecordEntity::getScore)));
        List<String> strengths = moduleScores.entrySet().stream()
                .filter(e -> e.getValue() >= 7).map(Map.Entry::getKey).toList();
        List<String> weaknesses = moduleScores.entrySet().stream()
                .filter(e -> e.getValue() <= 4).map(Map.Entry::getKey).toList();

        // 调 LLM 生成综合评语
        String modSummary = moduleScores.entrySet().stream()
                .map(e -> e.getKey() + "均分" + String.format("%.1f", e.getValue()))
                .collect(Collectors.joining(","));
        String prompt = """
                你是大厂校招面试导师。本轮背诵共%d题，总分%.0f，平均分%.1f。
                各模块均分：%s。
                请用50字以内给出鼓励和改进建议。
                """.formatted(count, total, avg, modSummary);

        String advice = callApi(prompt);
        return new SessionReportVO(total, avg, count, strengths, weaknesses, advice);
    }

    // ---- 内部 ----

    private String callApi(String prompt) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", "deepseek-chat");
            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", prompt);
            messages.add(msg);
            body.add("messages", messages);

            Request request = new Request.Builder()
                    .url(chatUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("DeepSeek API error {}: {}", response.code(),
                            response.body() != null ? response.body().string() : "");
                    return "";
                }
                String resp = response.body() != null ? response.body().string() : "";
                JsonObject root = gson.fromJson(resp, JsonObject.class);
                JsonArray choices = root.getAsJsonArray("choices");
                return choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message").get("content").getAsString();
            }
        } catch (IOException e) {
            log.error("DeepSeek API 调用失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /** 清理 markdown 代码块包裹 */
    private String extractJson(String raw) {
        if (raw == null || raw.isEmpty()) return "{}";
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("```[a-z]*\\s*", "").replaceAll("```", "").trim();
        }
        return s;
    }

    // ==== Phase 7: 学习档案报告（含历史上下文） ====

    @Override
    public String generateJournalReport(List<ReciteRecordEntity> records,
                                         List<String> recentJournalSummaries) {
        if (records == null || records.isEmpty()) return "{}";

        double total = records.stream().filter(r -> r.getScore() != null)
                .mapToInt(ReciteRecordEntity::getScore).sum();
        double avg = records.stream().filter(r -> r.getScore() != null)
                .mapToInt(ReciteRecordEntity::getScore).average().orElse(0);
        int count = records.size();

        // 按模块分组
        var moduleScores = records.stream()
                .filter(r -> r.getScore() != null)
                .collect(Collectors.groupingBy(ReciteRecordEntity::getModuleKey,
                        Collectors.averagingInt(ReciteRecordEntity::getScore)));

        String modSummary = moduleScores.entrySet().stream()
                .map(e -> e.getKey() + "均分" + String.format("%.1f", e.getValue()))
                .collect(Collectors.joining(","));

        // 历史上下文
        String historyContext = (recentJournalSummaries != null && !recentJournalSummaries.isEmpty())
                ? recentJournalSummaries.stream().limit(5)
                    .map(s -> s.length() > 200 ? s.substring(0, 200) + "..." : s)
                    .collect(Collectors.joining("\n"))
                : "无历史记录";

        String prompt = """
                你是学习顾问。根据以下背诵记录和最近学习档案生成报告。

                【本轮记录】共%d题，总分%.0f，平均分%.1f。各模块均分：%s。
                【最近5次档案】%s

                请严格返回以下JSON格式（不要markdown包裹）：
                {"summary":"一句话总结","strengths":["优势1","优势2"],"weaknesses":["薄弱1"],"advice":"综合建议","trendComment":"趋势分析","weakTags":["标签1"],"moduleScores":[{"moduleKey":"","moduleName":"","avgScore":0,"count":0}]}
                """.formatted(count, total, avg, modSummary, historyContext);

        String raw = callApi(prompt);
        return extractJson(raw);
    }
}
