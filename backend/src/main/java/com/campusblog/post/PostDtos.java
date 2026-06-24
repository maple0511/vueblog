package com.campusblog.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class PostDtos {
    private PostDtos() {
    }

    public record SavePostRequest(
            @NotBlank @Size(max = 100, message = "标题不能超过100个字符") String title,
            @Size(max = 500, message = "摘要不能超过500个字符") String summary,
            @NotBlank @Size(max = 50000) String content,
            @Size(max = 150, message = "AI摘要不能超过150个字符") String aiSummary,
            @Size(max = 5, message = "最多只能填写5个标签")
            List<@Size(max = 20, message = "每个标签不能超过20个字符") String> tags) {
    }

    public record TagView(Long id, String name, String source) {}

    public record PostView(Long id, Long authorId, String authorName, String title, String summary,
                           String content, String aiSummary, String aiMetadataStatus,
                           boolean aiSummaryEdited, LocalDateTime aiGeneratedAt,
                           String reviewStatus, String reviewReason,
                           List<TagView> tags, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record AiMetadataView(String status, String aiSummary, boolean edited,
                                 LocalDateTime generatedAt, List<TagView> tags) {}

    public record CommentRequest(@NotBlank @Size(max = 1000) String content) {}

    public record CommentView(Long id, Long authorId, String authorName, String content,
                              LocalDateTime createdAt) {}
}
