package com.campusblog.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleProviderTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void usesDashScopeCompatiblePathAndThinkingBody() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/compatible-mode/v1/chat/completions", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"choices":[{"message":{"content":"测试回复"}}],
                     "usage":{"prompt_tokens":12,"completion_tokens":4}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiProperties properties = properties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/compatible-mode/v1");
        ObjectMapper objectMapper = new ObjectMapper();
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(properties, objectMapper);

        AiProvider.AiResult result = provider.complete("系统提示", "用户问题");
        JsonNode body = objectMapper.readTree(requestBody.get());

        assertEquals("/compatible-mode/v1/chat/completions", path.get());
        assertEquals("qwen3.7-plus", body.path("model").asText());
        assertTrue(body.path("enable_thinking").asBoolean());
        assertEquals("测试回复", result.content());
        assertEquals(12, result.promptTokens());
    }

    @Test
    void streamsAnswerContentWithoutExposingReasoningContent() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = """
                    data: {"choices":[{"delta":{"reasoning_content":"内部思考"}}]}

                    data: {"choices":[{"delta":{"content":"文章"}}]}

                    data: {"choices":[{"delta":{"content":"回答"}}]}

                    data: [DONE]

                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiProperties properties = properties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(properties, new ObjectMapper());
        StringBuilder output = new StringBuilder();

        provider.stream("系统提示", "用户问题", output::append, () -> false);

        assertEquals("文章回答", output.toString());
    }

    private AiProperties properties() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel("qwen3.7-plus");
        properties.setEnableThinking(true);
        properties.setTimeoutSeconds(5);
        return properties;
    }
}
