package com.campusblog.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusblog.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AiUsageService {
    private final AiRequestLogMapper logMapper;
    private final AiProperties properties;
    private final AiProvider provider;

    public AiUsageService(AiRequestLogMapper logMapper, AiProperties properties, AiProvider provider) {
        this.logMapper = logMapper;
        this.properties = properties;
        this.provider = provider;
    }

    public void assertWithinLimit(Long userId) {
        long count = logMapper.selectCount(new LambdaQueryWrapper<AiRequestLog>()
                .eq(AiRequestLog::getUserId, userId)
                .ge(AiRequestLog::getCreatedAt, LocalDate.now().atStartOfDay()));
        if (count >= properties.getDailyLimitPerUser()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "今日 AI 使用次数已达上限");
        }
    }

    public void log(Long userId, Long postId, String feature, String status, long latency,
                    Integer promptTokens, Integer completionTokens, String errorCode) {
        AiRequestLog log = new AiRequestLog();
        log.setUserId(userId);
        log.setPostId(postId);
        log.setFeature(feature);
        log.setProvider(provider.providerName());
        log.setModel(properties.getModel());
        log.setStatus(status);
        log.setLatencyMs(latency);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setErrorCode(errorCode);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }
}
