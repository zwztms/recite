package cn.bugstack.recite;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = cn.bugstack.recite.app.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.MethodName.class)
@DisplayName("Recite 集成测试")
class ReciteControllerIT {

    @Autowired
    private MockMvc mvc;

    private static String token;

    @BeforeAll
    static void login(@Autowired MockMvc mvc) throws Exception {
        var resp = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"account":"15386747351","password":"zw123456"}
                    """))
                .andExpect(status().isOk())
                .andReturn();
        token = com.jayway.jsonpath.JsonPath.read(
                resp.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    @DisplayName("完整背诵流程: start → SSE answer → current-question → finish → history")
    void t01_fullReciteFlow() throws Exception {
        // 1. 开始背诵
        MvcResult startResp = mvc.perform(post("/recite/start")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"mode":"CATEGORY","moduleKeys":["jvm"],"count":1}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.question.id").isNotEmpty())
                .andExpect(jsonPath("$.data.totalQuestions").value(1))
                .andReturn();

        String sessionId = com.jayway.jsonpath.JsonPath.read(
                startResp.getResponse().getContentAsString(), "$.data.sessionId");
        String questionId = com.jayway.jsonpath.JsonPath.read(
                startResp.getResponse().getContentAsString(), "$.data.question.id");
        String qText = com.jayway.jsonpath.JsonPath.read(
                startResp.getResponse().getContentAsString(), "$.data.question.question");
        assertThat(sessionId).isNotEmpty();
        System.out.println("  [OK] start → " + sessionId.substring(0, 16) + "... Q="
                + (qText.length() > 40 ? qText.substring(0, 40) + "..." : qText));

        // 2. SSE 提交答案（异步线程处理，MockMvc 可能拿不到 Content-Type）
        mvc.perform(post("/recite/" + sessionId + "/answer")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"questionId":"%s","answer":"JVM has heap stack method area PC register native stack"}
                    """.formatted(questionId)))
                .andExpect(status().isOk());
        System.out.println("  [OK] SSE → 200 OK");

        // 等异步线程写入完成
        Thread.sleep(2000);

        // 3. 下一题（当前题）
        mvc.perform(get("/recite/" + sessionId + "/current-question")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.question").isNotEmpty());
        System.out.println("  [OK] current-question → 200");

        // 4. 结束背诵
        mvc.perform(post("/recite/" + sessionId + "/finish")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.averageScore").exists());
        System.out.println("  [OK] finish → statistics returned");

        // 5. 查看历史
        mvc.perform(get("/recite/history?limit=5")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").isArray());
        System.out.println("  [OK] history → array returned");
    }
}
