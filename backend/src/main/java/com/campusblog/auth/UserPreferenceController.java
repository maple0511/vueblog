package com.campusblog.auth;

import com.campusblog.common.ApiResponse;
import com.campusblog.common.PageResult;
import com.campusblog.post.PostDtos;
import com.campusblog.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserPreferenceController {
    private final UserPreferenceService service;

    public UserPreferenceController(UserPreferenceService service) {
        this.service = service;
    }

    @GetMapping("/users/preferences")
    ApiResponse<AuthDtos.PreferenceOptions> options() {
        return ApiResponse.ok(service.options(SecurityUtils.currentUser()));
    }

    @PutMapping("/users/preferences")
    ApiResponse<AuthDtos.PreferenceOptions> save(@Valid @RequestBody AuthDtos.PreferenceRequest request) {
        return ApiResponse.ok(service.save(SecurityUtils.currentUser(), request));
    }

    @GetMapping("/recommendations/posts")
    ApiResponse<PageResult<PostDtos.PostView>> recommendations(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) long size) {
        return ApiResponse.ok(service.recommendations(SecurityUtils.currentUser(), page, size));
    }
}
