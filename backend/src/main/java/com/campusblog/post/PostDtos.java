package com.campusblog.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class PostDtos {
    private PostDtos() {
    }

    public record SavePostRequest(
            @NotBlank @Size(max = 100) String title,
            @Size(max = 500) String summary,
            @NotBlank @Size(max = 50000) String content,
            @Size(max = 150) String aiSummary,
            List<@Size(max = 20) String> tags) {
    }

    public record TagView(Long id, String name, String source) {}

    public record PostView(Long id, Long authorId, String authorName, String title, String summary,
                           String content, String aiSummary, String aiMetadataStatus,
                           boolean aiSummaryEdited, LocalDateTime aiGeneratedAt,
                           List<TagView> tags, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record AiMetadataView(String status, String aiSummary, boolean edited,
                                 LocalDateTime generatedAt, List<TagView> tags) {}

    public record CommentRequest(@NotBlank @Size(max = 1000) String content) {}

    public record CommentView(Long id, Long authorId, String authorName, String content,
                              LocalDateTime createdAt) {}
}

