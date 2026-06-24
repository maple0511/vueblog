package com.campusblog.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusblog.auth.User;
import com.campusblog.auth.UserMapper;
import com.campusblog.common.BusinessException;
import com.campusblog.common.PageResult;
import com.campusblog.post.PostDtos;
import com.campusblog.post.PostService;
import com.campusblog.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final PostService postService;

    public AdminService(UserMapper userMapper, PostService postService) {
        this.userMapper = userMapper;
        this.postService = postService;
    }

    public PageResult<AdminDtos.UserAdminView> users(String keyword, String status, long page, long size) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like(User::getUsername, keyword).or().like(User::getEmail, keyword));
        }
        if (StringUtils.hasText(status)) {
            query.eq(User::getStatus, status);
        }
        IPage<User> result = userMapper.selectPage(Page.of(page, Math.min(size, 50)), query);
        return new PageResult<>(result.getRecords().stream().map(user -> new AdminDtos.UserAdminView(
                        user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus(),
                        Boolean.TRUE.equals(user.getProfileCompleted()), user.getCreatedAt().toString()))
                .toList(), result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Transactional
    public AdminDtos.UserAdminView updateUserStatus(Long id, String status, AuthUser admin) {
        if (!List.of("ACTIVE", "DISABLED").contains(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "成员状态不合法");
        }
        if (id.equals(admin.id()) && "DISABLED".equals(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不能停用当前管理员账号");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "成员不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return new AdminDtos.UserAdminView(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getStatus(), Boolean.TRUE.equals(user.getProfileCompleted()), user.getCreatedAt().toString());
    }

    public PageResult<PostDtos.PostView> posts(String keyword, String reviewStatus, long page, long size) {
        return postService.listForAdmin(keyword, reviewStatus, page, size);
    }

    public PostDtos.PostView reviewPost(Long id, AdminDtos.PostReviewRequest request, AuthUser admin) {
        return postService.review(id, request.reviewStatus(), request.reason(), admin);
    }
}
