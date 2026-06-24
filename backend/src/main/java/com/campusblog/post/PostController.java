package com.campusblog.post;

import com.campusblog.ai.AiMetadataService;
import com.campusblog.common.ApiResponse;
import com.campusblog.common.PageResult;
import com.campusblog.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final AiMetadataService aiMetadataService;

    public PostController(PostService postService, CommentService commentService,
                          AiMetadataService aiMetadataService) {
        this.postService = postService;
        this.commentService = commentService;
        this.aiMetadataService = aiMetadataService;
    }

    @GetMapping("/posts")
    ApiResponse<PageResult<PostDtos.PostView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) long size) {
        return ApiResponse.ok(postService.list(keyword, tag, page, size));
    }

    @GetMapping("/posts/{id}")
    ApiResponse<PostDtos.PostView> get(@PathVariable Long id) {
        return ApiResponse.ok(postService.get(id, SecurityUtils.optionalUser()));
    }

    @PostMapping("/posts")
    ResponseEntity<ApiResponse<PostDtos.PostView>> create(@Valid @RequestBody PostDtos.SavePostRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(postService.create(request, SecurityUtils.currentUser())));
    }

    @PutMapping("/posts/{id}")
    ApiResponse<PostDtos.PostView> update(@PathVariable Long id,
                                          @Valid @RequestBody PostDtos.SavePostRequest request) {
        return ApiResponse.ok(postService.update(id, request, SecurityUtils.currentUser()));
    }

    @DeleteMapping("/posts/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id, SecurityUtils.currentUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    ApiResponse<List<PostDtos.TagView>> tags() {
        return ApiResponse.ok(postService.allTags());
    }

    @GetMapping("/posts/{id}/comments")
    ApiResponse<List<PostDtos.CommentView>> comments(@PathVariable Long id) {
        return ApiResponse.ok(commentService.list(id, SecurityUtils.optionalUser()));
    }

    @PostMapping("/posts/{id}/comments")
    ResponseEntity<ApiResponse<PostDtos.CommentView>> comment(
            @PathVariable Long id, @Valid @RequestBody PostDtos.CommentRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(commentService.create(id, request, SecurityUtils.currentUser())));
    }

    @DeleteMapping("/comments/{id}")
    ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id, SecurityUtils.currentUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{id}/ai-metadata/status")
    ApiResponse<PostDtos.AiMetadataView> metadata(@PathVariable Long id) {
        return ApiResponse.ok(postService.metadata(id));
    }

    @PostMapping("/posts/{id}/ai-metadata/regenerate")
    ResponseEntity<Void> regenerate(@PathVariable Long id) {
        aiMetadataService.requestRegeneration(id, SecurityUtils.currentUser());
        return ResponseEntity.accepted().build();
    }
}
