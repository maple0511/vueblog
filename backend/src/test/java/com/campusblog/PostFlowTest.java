package com.campusblog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campusblog.auth.User;
import com.campusblog.auth.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostFlowTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserMapper userMapper;

    @Test
    void createSearchAndAiFailureFallback() throws Exception {
        String token = register("writer", "writer@example.com");
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "校园实验室开放指南",
                "summary", "人工摘要",
                "content", "# 开放时间\n实验室每周一开放。",
                "tags", new String[]{"校园", "实验室"}));

        String response = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long postId = objectMapper.readTree(response).at("/data/id").asLong();

        mockMvc.perform(get("/api/posts").param("keyword", "开放指南"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(postId));

        String aiStatus = "";
        for (int attempt = 0; attempt < 20 && !"FAILED".equals(aiStatus); attempt++) {
            Thread.sleep(50);
            String metadata = mockMvc.perform(get("/api/posts/{id}/ai-metadata/status", postId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            aiStatus = objectMapper.readTree(metadata).at("/data/status").asText();
        }
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", aiStatus);
    }

    @Test
    void adminCanReviewPostsAndRecommendationsUsePreferences() throws Exception {
        register("reviewer", "reviewer@example.com");
        User reviewer = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "reviewer"));
        reviewer.setRole("ADMIN");
        userMapper.updateById(reviewer);
        String adminToken = login("reviewer", "Campus123!");
        String studentToken = register("student-reco", "student-reco@example.com");

        long foodPostId = createPost(studentToken, "校园美食地图", "食堂窗口推荐", "# 美食\n二食堂有新窗口。", "美食");
        createPost(studentToken, "校园体育活动", "本周球赛安排", "# 体育\n周五有篮球赛。", "体育");

        mockMvc.perform(put("/api/users/preferences")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tags", new String[]{"美食"}))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recommendations/posts")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(foodPostId));

        mockMvc.perform(put("/api/admin/posts/{id}/review", foodPostId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reviewStatus", "HIDDEN", "reason", "演示隐藏"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("HIDDEN"));

        mockMvc.perform(get("/api/posts").param("keyword", "校园美食地图"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    private long createPost(String token, String title, String summary, String content, String tag) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "summary", summary,
                "content", content,
                "tags", new String[]{tag}));
        String response = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private String register(String username, String email) throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", email, "password", "Campus123!"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.at("/data/token").asText();
    }

    private String login(String account, String password) throws Exception {
        String request = objectMapper.writeValueAsString(Map.of("account", account, "password", password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/token").asText();
    }
}
