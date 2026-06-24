package com.campusblog.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String account, @NotBlank String password) {
    }

    public record UserView(Long id, String username, String email, String role, String status, boolean profileCompleted) {
    }

    public record AuthView(String token, UserView user) {
    }

    public record PreferenceOptions(List<String> options, List<String> selected) {
    }

    public record PreferenceRequest(
            @Size(max = 10, message = "最多只能选择10个兴趣标签")
            List<@NotBlank @Size(max = 20, message = "每个标签不能超过20个字符") String> tags) {
    }
}
