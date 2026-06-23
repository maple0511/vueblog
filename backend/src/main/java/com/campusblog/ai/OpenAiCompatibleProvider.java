package com.campusblog.ai;

import com.campusblog.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Component
public class OpenAiCompatibleProvider implements AiProvider {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public OpenAiCompatibleProvider(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(properties.getTimeoutSeconds(), 20))).build();
    }

    @Override
    public AiResult complete(String systemPrompt, String userPrompt) {
        ensureEnabled();
        try {
            String body = objectMapper.writeValueAsString(requestBody(systemPrompt, userPrompt, false));
            HttpResponse<String> response = client.send(request(body),
                    HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response.statusCode(), response.body());
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.at("/choices/0/message/content").asText();
            return new AiResult(content, nullableInt(root.at("/usage/prompt_tokens")),
                    nullableInt(root.at("/usage/completion_tokens")));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI 服务暂时不可用");
        }
    }

    @Override
    public void stream(String systemPrompt, String userPrompt, Consumer<String> chunkConsumer,
                       BooleanSupplier cancelled) {
        ensureEnabled();
        try {
            String body = objectMapper.writeValueAsString(requestBody(systemPrompt, userPrompt, true));
            HttpResponse<java.io.InputStream> response = client.send(request(body),
                    HttpResponse.BodyHandlers.ofInputStream());
            ensureSuccess(response.statusCode(), "");
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(response.body()))) {
                String line;
                while (!cancelled.getAsBoolean() && (line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    JsonNode node = objectMapper.readTree(data);
                    String content = node.at("/choices/0/delta/content").asText("");
                    if (!content.isEmpty()) chunkConsumer.accept(content);
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI 流式服务暂时不可用");
        }
    }

    @Override
    public String providerName() {
        return "openai-compatible";
    }

    private Map<String, Object> requestBody(String systemPrompt, String userPrompt, boolean stream) {
        return Map.of(
                "model", properties.getModel(),
                "stream", stream,
                "temperature", 0.4,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
    }

    private HttpRequest request(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl().replaceAll("/$", "") + "/v1/chat/completions"))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private void ensureEnabled() {
        if (!properties.isEnabled() || properties.getApiKey().isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI 功能尚未配置");
        }
    }

    private void ensureSuccess(int status, String body) {
        if (status == 429) throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "AI 服务请求过于频繁");
        if (status < 200 || status >= 300) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI 服务返回异常");
        }
    }

    private Integer nullableInt(JsonNode node) {
        return node.isMissingNode() ? null : node.asInt();
    }
}

