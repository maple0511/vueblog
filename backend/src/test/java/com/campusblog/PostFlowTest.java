package com.campusblog;

import com.fasterxml.jackson.databind.JsonNode;
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
class PostFlowTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createSearchAndAiFailureFallback() throws Exception {
        String token = register();
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

    private String register() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "username", "writer", "email", "writer@example.com", "password", "Campus123!"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.at("/data/token").asText();
    }
}
