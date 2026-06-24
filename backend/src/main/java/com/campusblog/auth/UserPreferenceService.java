package com.campusblog.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusblog.post.PostDtos;
import com.campusblog.post.PostMappers;
import com.campusblog.post.PostService;
import com.campusblog.post.Tag;
import com.campusblog.common.PageResult;
import com.campusblog.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class UserPreferenceService {
    private static final List<String> DEFAULT_OPTIONS = List.of("美食", "体育", "学习", "校园生活", "社团", "就业", "实验室", "竞赛");

    private final UserMapper userMapper;
    private final UserTagPreferenceMapper preferenceMapper;
    private final PostMappers.TagMapper tagMapper;
    private final PostService postService;

    public UserPreferenceService(UserMapper userMapper, UserTagPreferenceMapper preferenceMapper,
                                 PostMappers.TagMapper tagMapper, PostService postService) {
        this.userMapper = userMapper;
        this.preferenceMapper = preferenceMapper;
        this.tagMapper = tagMapper;
        this.postService = postService;
    }

    public AuthDtos.PreferenceOptions options(AuthUser user) {
        LinkedHashSet<String> options = new LinkedHashSet<>(DEFAULT_OPTIONS);
        tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName)).stream()
                .map(Tag::getName).forEach(options::add);
        return new AuthDtos.PreferenceOptions(List.copyOf(options), selected(user.id()));
    }

    @Transactional
    public AuthDtos.PreferenceOptions save(AuthUser user, AuthDtos.PreferenceRequest request) {
        preferenceMapper.delete(new LambdaQueryWrapper<UserTagPreference>().eq(UserTagPreference::getUserId, user.id()));
        List<String> tags = request.tags() == null ? List.of() : request.tags().stream()
                .filter(StringUtils::hasText).map(String::trim).distinct().limit(10).toList();
        LocalDateTime now = LocalDateTime.now();
        for (String tagName : tags) {
            UserTagPreference preference = new UserTagPreference();
            preference.setUserId(user.id());
            preference.setTagName(tagName);
            preference.setCreatedAt(now);
            preferenceMapper.insert(preference);
        }
        User entity = userMapper.selectById(user.id());
        entity.setProfileCompleted(true);
        userMapper.updateById(entity);
        return new AuthDtos.PreferenceOptions(options(user).options(), tags);
    }

    public PageResult<PostDtos.PostView> recommendations(AuthUser user, long page, long size) {
        return postService.recommend(selected(user.id()), page, size);
    }

    private List<String> selected(Long userId) {
        return preferenceMapper.selectList(new LambdaQueryWrapper<UserTagPreference>()
                        .eq(UserTagPreference::getUserId, userId).orderByAsc(UserTagPreference::getCreatedAt))
                .stream().map(UserTagPreference::getTagName).toList();
    }
}
