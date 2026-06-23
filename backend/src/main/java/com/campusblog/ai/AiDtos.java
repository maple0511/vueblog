package com.campusblog.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AiDtos {
    private AiDtos() {
    }

    public enum WritingAction { OUTLINE, CONTINUE, REWRITE, TITLE_SUGGESTIONS }

    public record WritingRequest(
            WritingAction action,
            @Size(max = 100) String title,
            @Size(max = 6000) String selectedText,
            @Size(max = 20000) String context) {}

    public record HistoryItem(@NotBlank @Size(max = 500) String question,
                              @NotBlank @Size(max = 3000) String answer) {}

    public record QuestionRequest(@NotBlank @Size(max = 500) String question,
                                  @Size(max = 5) List<HistoryItem> history) {}
}

