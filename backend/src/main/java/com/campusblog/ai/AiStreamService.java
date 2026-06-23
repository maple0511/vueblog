package com.campusblog.ai;

import com.campusblog.post.Post;
import com.campusblog.post.PostService;
import com.campusblog.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiStreamService {
    private final AiProvider provider;
    private final AiUsageService usageService;
    private final PostService postService;
    private final Executor executor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()), runnable -> {
                Thread thread = new Thread(runnable, "campusblog-ai-stream");
                thread.setDaemon(true);
                return thread;
            });

    public AiStreamService(AiProvider provider, AiUsageService usageService, PostService postService) {
        this.provider = provider;
        this.usageService = usageService;
        this.postService = postService;
    }

    public SseEmitter writing(AiDtos.WritingRequest request, AuthUser user) {
        String prompt = writingPrompt(request);
        return stream(user.id(), null, "WRITING",
                "你是校园知识博客写作助手。输出中文，保持事实准确，不虚构来源。", prompt);
    }

    public SseEmitter question(Long postId, AiDtos.QuestionRequest request, AuthUser user) {
        Post post = postService.requirePost(postId);
        String history = request.history() == null ? "" : request.history().stream()
                .map(item -> "问：" + item.question() + "\n答：" + item.answer()).reduce("", (a, b) -> a + "\n" + b);
        String prompt = """
                文章标题：%s
                文章摘要：%s
                文章正文：
                %s

                最近对话：
                %s

                当前问题：%s
                """.formatted(post.getTitle(), post.getAiSummary() == null ? post.getSummary() : post.getAiSummary(),
                post.getContent(), history, request.question());
        return stream(user.id(), postId, "QUESTION",
                "你只能依据给定文章回答。文章没有答案时必须明确回复“该文章未提供相关信息”。"
                        + "如能定位，请在结尾列出相关章节标题；不得伪造章节或引用。", prompt);
    }

    private SseEmitter stream(Long userId, Long postId, String feature, String system, String prompt) {
        usageService.assertWithinLimit(userId);
        SseEmitter emitter = new SseEmitter(65_000L);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        executor.execute(() -> {
            long started = System.currentTimeMillis();
            try {
                provider.stream(system, prompt, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (Exception exception) {
                        cancelled.set(true);
                    }
                }, cancelled::get);
                if (!cancelled.get()) {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                }
                usageService.log(userId, postId, feature, "SUCCESS",
                        System.currentTimeMillis() - started, null, null, null);
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(exception.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.complete();
                usageService.log(userId, postId, feature, "FAILED",
                        System.currentTimeMillis() - started, null, null, exception.getClass().getSimpleName());
            }
        });
        return emitter;
    }

    private String writingPrompt(AiDtos.WritingRequest request) {
        if (request.action() == null) throw new IllegalArgumentException("action is required");
        return switch (request.action()) {
            case OUTLINE -> "请为题目生成结构清晰的 Markdown 大纲。\n题目：" + request.title()
                    + "\n已有内容：" + safe(request.context());
            case CONTINUE -> "请延续现有文章，输出可直接插入的 Markdown 正文。\n题目：" + request.title()
                    + "\n现有正文：" + safe(request.context());
            case REWRITE -> "请改写选中文字，保持原意并提升清晰度，只输出改写结果。\n"
                    + safe(request.selectedText());
            case TITLE_SUGGESTIONS -> "根据内容给出3个具体、克制的中文标题，每行一个。\n"
                    + safe(request.context());
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
