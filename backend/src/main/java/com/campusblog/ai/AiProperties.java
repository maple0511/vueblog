package com.campusblog.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private boolean enabled;
    private String baseUrl = "https://ws-etymarnalsjn28ue.cn-beijing.maas.aliyuncs.com/compatible-mode/v1";
    private String apiKey = "";
    private String model = "qwen3.7-plus";
    private boolean enableThinking = true;
    private int timeoutSeconds = 60;
    private int dailyLimitPerUser = 50;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public boolean isEnableThinking() { return enableThinking; }
    public void setEnableThinking(boolean enableThinking) { this.enableThinking = enableThinking; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getDailyLimitPerUser() { return dailyLimitPerUser; }
    public void setDailyLimitPerUser(int dailyLimitPerUser) { this.dailyLimitPerUser = dailyLimitPerUser; }
}
