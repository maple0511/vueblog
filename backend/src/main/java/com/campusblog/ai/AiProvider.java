package com.campusblog.ai;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public interface AiProvider {
    AiResult complete(String systemPrompt, String userPrompt);
    void stream(String systemPrompt, String userPrompt, Consumer<String> chunkConsumer,
                BooleanSupplier cancelled);
    String providerName();

    record AiResult(String content, Integer promptTokens, Integer completionTokens) {}
}

