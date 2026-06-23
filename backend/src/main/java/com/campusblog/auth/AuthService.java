package com.campusblog.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusblog.common.BusinessException;
import com.campusblog.security.AuthUser;
import com.campusblog.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthView register(AuthDtos.RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()).or().eq(User::getEmail, request.email()));
        if (count > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名或邮箱已被使用");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return createAuthView(user);
    }

    public AuthDtos.AuthView login(AuthDtos.LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.account()).or().eq(User::getEmail, request.account()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        return createAuthView(user);
    }

    public AuthDtos.UserView current(AuthUser authUser) {
        User user = userMapper.selectById(authUser.id());
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return view(user);
    }

    private AuthDtos.AuthView createAuthView(User user) {
        return new AuthDtos.AuthView(jwtService.create(user.getId(), user.getUsername()), view(user));
    }

    private AuthDtos.UserView view(User user) {
        return new AuthDtos.UserView(user.getId(), user.getUsername(), user.getEmail());
    }
}

