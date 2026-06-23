package com.campusblog.auth;

import com.campusblog.common.ApiResponse;
import com.campusblog.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthDtos.AuthView>> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(authService.register(request)));
    }

    @PostMapping("/login")
    ApiResponse<AuthDtos.AuthView> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    ApiResponse<AuthDtos.UserView> me() {
        return ApiResponse.ok(authService.current(SecurityUtils.currentUser()));
    }
}

