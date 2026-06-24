package com.campusblog.admin;

import com.campusblog.common.ApiResponse;
import com.campusblog.common.PageResult;
import com.campusblog.post.PostDtos;
import com.campusblog.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/users")
    ApiResponse<PageResult<AdminDtos.UserAdminView>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) long size) {
        return ApiResponse.ok(service.users(keyword, status, page, size));
    }

    @PutMapping("/users/{id}/status")
    ApiResponse<AdminDtos.UserAdminView> updateUserStatus(
            @PathVariable Long id, @Valid @RequestBody AdminDtos.UserStatusRequest request) {
        return ApiResponse.ok(service.updateUserStatus(id, request.status(), SecurityUtils.currentUser()));
    }

    @GetMapping("/posts")
    ApiResponse<PageResult<PostDtos.PostView>> posts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) long size) {
        return ApiResponse.ok(service.posts(keyword, reviewStatus, page, size));
    }

    @PutMapping("/posts/{id}/review")
    ApiResponse<PostDtos.PostView> reviewPost(
            @PathVariable Long id, @Valid @RequestBody AdminDtos.PostReviewRequest request) {
        return ApiResponse.ok(service.reviewPost(id, request, SecurityUtils.currentUser()));
    }
}
