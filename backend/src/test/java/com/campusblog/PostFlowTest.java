package com.campusblog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campusblog.ai.AiRequestLog;
import com.campusblog.ai.AiRequestLogMapper;
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
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    @Autowired AiRequestLogMapper aiRequestLogMapper;

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

    @Test
    void rejectsAnonymousAndCrossOwnerPostOperations() throws Exception {
        String authorToken = register("author" + System.nanoTime(), "author" + System.nanoTime() + "@example.com");
        String otherToken = register("other" + System.nanoTime(), "other" + System.nanoTime() + "@example.com");
        long postId = createPost(authorToken, "越权测试文章", "摘要", "# 正文\n作者内容。", "学习");

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "匿名文章", "content", "匿名正文", "tags", new String[]{"学习"}))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/posts/{id}", postId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "他人修改", "content", "不允许", "tags", new String[]{"学习"}))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能操作自己的文章"));
    }

    @Test
    void commentsCanBeDeletedByCommentAuthorOrPostAuthorOnly() throws Exception {
        String authorToken = register("post-owner" + System.nanoTime(), "post-owner" + System.nanoTime() + "@example.com");
        String commenterToken = register("commenter" + System.nanoTime(), "commenter" + System.nanoTime() + "@example.com");
        String strangerToken = register("stranger" + System.nanoTime(), "stranger" + System.nanoTime() + "@example.com");
        long postId = createPost(authorToken, "评论权限测试", "摘要", "# 正文\n评论测试。", "校园");

        String response = mockMvc.perform(post("/api/posts/{id}/comments", postId)
                        .header("Authorization", "Bearer " + commenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "这是一条评论"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("这是一条评论"))
                .andReturn().getResponse().getContentAsString();
        long commentId = objectMapper.readTree(response).at("/data/id").asLong();

        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能删除自己的评论或自己文章下的评论"));

        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{id}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void validationRejectsLongTagAndTooManyPreferenceTags() throws Exception {
        String token = register("validator" + System.nanoTime(), "validator" + System.nanoTime() + "@example.com");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "标签校验",
                                "content", "正文",
                                "tags", new String[]{"这个标签名字明显已经超过二十个字符长度限制"}))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("每个标签不能超过20个字符"));

        mockMvc.perform(put("/api/users/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tags", new String[]{"1","2","3","4","5","6","7","8","9","10","11"}))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("最多只能选择10个兴趣标签"));
    }

    @Test
    void nonAdminIsForbiddenAndAiQuotaReturns429() throws Exception {
        String token = register("limited" + System.nanoTime(), "limited" + System.nanoTime() + "@example.com");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, objectMapper.readTree(
                mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()).at("/data/username").asText()));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        IntStream.range(0, 50).forEach(index -> {
            AiRequestLog log = new AiRequestLog();
            log.setUserId(user.getId());
            log.setFeature("WRITING");
            log.setProvider("test");
            log.setModel("test-model");
            log.setStatus("SUCCESS");
            log.setLatencyMs(1L);
            log.setCreatedAt(LocalDateTime.now());
            aiRequestLogMapper.insert(log);
        });

        mockMvc.perform(post("/api/ai/writing/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "OUTLINE", "title", "限流测试", "context", "正文"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("今日 AI 使用次数已达上限"));
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
