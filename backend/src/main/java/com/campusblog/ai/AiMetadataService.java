package com.campusblog.ai;

import com.campusblog.common.BusinessException;
import com.campusblog.post.Post;
import com.campusblog.post.PostMappers;
import com.campusblog.post.PostService;
import com.campusblog.security.AuthUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiMetadataService {
    private static final String SYSTEM_PROMPT = """
            你是校园知识博客的内容编辑。只根据用户提供的文章输出严格 JSON：
            {"summary":"80到150字中文摘要","tags":["标签1","标签2"]}
            标签必须为2到5个、每个不超过10个汉字。禁止输出 Markdown 或额外解释。
            """;

    private final AiProvider provider;
    private final ObjectMapper objectMapper;
    private final PostMappers.PostMapper postMapper;
    private final PostService postService;
    private final AiUsageService usageService;
    private final ApplicationEventPublisher eventPublisher;

    public AiMetadataService(AiProvider provider, ObjectMapper objectMapper,
                             PostMappers.PostMapper postMapper, PostService postService,
                             AiUsageService usageService, ApplicationEventPublisher eventPublisher) {
        this.provider = provider;
        this.objectMapper = objectMapper;
        this.postMapper = postMapper;
        this.postService = postService;
        this.usageService = usageService;
        this.eventPublisher = eventPublisher;
    }

    public void requestRegeneration(Long postId, AuthUser user) {
        Post post = postService.requireOwned(postId, user.id());
        post.setAiMetadataStatus("PENDING");
        postMapper.updateById(post);
        requestGeneration(postId, user.id());
    }

    public void requestGeneration(Long postId, Long userId) {
        eventPublisher.publishEvent(new AiMetadataRequested(postId, userId));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAsync(AiMetadataRequested request) {
        Post post = postMapper.selectById(request.postId());
        if (post == null) return;
        long started = System.currentTimeMillis();
        try {
            usageService.assertWithinLimit(request.userId());
            AiProvider.AiResult result = provider.complete(SYSTEM_PROMPT,
                    "标题：" + post.getTitle() + "\n正文：\n" + post.getContent());
            Metadata metadata = parse(result.content());
            post.setAiSummary(metadata.summary());
            post.setAiSummaryEdited(false);
            post.setAiMetadataStatus("READY");
            post.setAiGeneratedAt(LocalDateTime.now());
            postMapper.updateById(post);
            postService.replaceAiTags(request.postId(), metadata.tags());
            usageService.log(request.userId(), request.postId(), "METADATA", "SUCCESS",
                    System.currentTimeMillis() - started, result.promptTokens(), result.completionTokens(), null);
        } catch (Exception exception) {
            post.setAiMetadataStatus("FAILED");
            postMapper.updateById(post);
            usageService.log(request.userId(), request.postId(), "METADATA", "FAILED",
                    System.currentTimeMillis() - started, null, null, exception.getClass().getSimpleName());
        }
    }

    private Metadata parse(String content) throws Exception {
        String normalized = content.replace("```json", "").replace("```", "").trim();
        JsonNode root = objectMapper.readTree(normalized);
        String summary = root.path("summary").asText("").trim();
        if (summary.length() < 40 || summary.length() > 180) {
            throw new IllegalArgumentException("AI summary length invalid");
        }
        List<String> tags = new ArrayList<>();
        root.path("tags").forEach(node -> {
            String value = node.asText("").trim();
            if (!value.isBlank() && value.length() <= 20 && tags.size() < 5) tags.add(value);
        });
        if (tags.size() < 2) throw new IllegalArgumentException("AI tags invalid");
        return new Metadata(summary, tags);
    }

    private record Metadata(String summary, List<String> tags) {}
}
