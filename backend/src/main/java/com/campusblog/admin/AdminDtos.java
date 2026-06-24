package com.campusblog.admin;

import com.campusblog.auth.AuthDtos;
import com.campusblog.post.PostDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record UserStatusRequest(@NotBlank String status) {
    }

    public record PostReviewRequest(
            @NotBlank String reviewStatus,
            @Size(max = 300, message = "审核说明不能超过300个字符") String reason) {
    }

    public record UserAdminView(Long id, String username, String email, String role, String status,
                                boolean profileCompleted, String createdAt) {
        public static UserAdminView from(AuthDtos.UserView user, String createdAt) {
            return new UserAdminView(user.id(), user.username(), user.email(), user.role(),
                    user.status(), user.profileCompleted(), createdAt);
        }
    }

    public record PostAdminView(PostDtos.PostView post) {
    }
}
