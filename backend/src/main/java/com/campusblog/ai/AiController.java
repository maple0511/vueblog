package com.campusblog.ai;

import com.campusblog.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class AiController {
    private final AiStreamService streamService;

    public AiController(AiStreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping(value = "/ai/writing/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter writing(@Valid @RequestBody AiDtos.WritingRequest request) {
        return streamService.writing(request, SecurityUtils.currentUser());
    }

    @PostMapping(value = "/posts/{id}/questions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter question(@PathVariable Long id, @Valid @RequestBody AiDtos.QuestionRequest request) {
        return streamService.question(id, request, SecurityUtils.currentUser());
    }
}

