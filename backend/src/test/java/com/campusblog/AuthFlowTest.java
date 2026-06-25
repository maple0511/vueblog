package com.campusblog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusblog.auth.User;
import com.campusblog.auth.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserMapper userMapper;

    @Test
    void registerLoginAndProtectedEndpoint() throws Exception {
        String register = objectMapper.writeValueAsString(Map.of(
                "username", "student", "email", "student@example.com", "password", "Campus123!"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(register))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.username").value("student"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).at("/data/token").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("student@example.com"));
    }

    @Test
    void protectedEndpointRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegisterAndInvalidLoginAreRejected() throws Exception {
        String username = "dup" + System.nanoTime();
        String email = username + "@example.com";
        String register = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", email, "password", "Campus123!"));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(register))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(register))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("用户名或邮箱已被使用"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("account", username, "password", "wrong-pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        String username = "disabled" + System.nanoTime();
        String email = username + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "email", email, "password", "Campus123!"))))
                .andExpect(status().isCreated());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        user.setStatus("DISABLED");
        userMapper.updateById(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("account", username, "password", "Campus123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("账号已被管理员停用"));
    }
}
