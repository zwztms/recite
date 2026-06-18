package cn.bugstack.recite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = cn.bugstack.recite.app.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.MethodName.class)
@DisplayName("Auth 集成测试")
class AuthControllerIT {

    @Autowired
    private MockMvc mvc;

    private static String token;
    private static String nickname;

    @Test
    @DisplayName("1. 注册新用户 → 返回 token+role+nickname")
    void t01_register() throws Exception {
        String phone = "138" + System.currentTimeMillis() % 100000000;
        var resp = mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"%s","password":"test123","nickname":"集成测试"}
                    """.formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.nickname").value("集成测试"))
                .andReturn();

        token = com.jayway.jsonpath.JsonPath.read(
                resp.getResponse().getContentAsString(), "$.data.token");
        nickname = com.jayway.jsonpath.JsonPath.read(
                resp.getResponse().getContentAsString(), "$.data.nickname");

        assertThat(token).isNotEmpty();
        System.out.println("  [OK] register → token=" + token.substring(0, 20) + "...");
    }

    @Test
    @DisplayName("2. 用已有账号登录 → 返回 token")
    void t02_login() throws Exception {
        var resp = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"account":"15386747351","password":"zw123456"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String loginToken = com.jayway.jsonpath.JsonPath.read(
                resp.getResponse().getContentAsString(), "$.data.token");
        System.out.println("  [OK] login → token=" + loginToken.substring(0, 20) + "...");
    }

    @Test
    @DisplayName("3. 无 token 访问背诵 → 被 Sa-Token 拦截")
    void t03_noTokenShouldBeForbidden() throws Exception {
        var resp = mvc.perform(post("/recite/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"mode":"CATEGORY","moduleKeys":["jvm"],"count":1}
                    """))
                .andReturn();

        // Sa-Token 拦截后 GlobalExceptionHandler 返回 500 或 Sa-Token 返回业务错误
        String body = resp.getResponse().getContentAsString();
        String code = com.jayway.jsonpath.JsonPath.read(body, "$.code");
        assertThat(code).isIn("500", "401");
        System.out.println("  [OK] no-token → code=" + code + " (intercepted)");
    }

    @Test
    @DisplayName("4. 错误密码 → 401")
    void t04_wrongPasswordShouldFail() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"account":"15386747351","password":"WRONG"}
                    """))
                .andExpect(status().isOk())  // HTTP 200, 业务码 401
                .andExpect(jsonPath("$.code").value("401"));
        System.out.println("  [OK] wrong-password → code=401");
    }
}
