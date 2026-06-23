package com.campusblog.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusblog.ai.AiMetadataService;
import com.campusblog.auth.User;
import com.campusblog.auth.UserMapper;
import com.campusblog.common.BusinessException;
import com.campusblog.common.PageResult;
import com.campusblog.security.AuthUser;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PostService {
    private final PostMappers.PostMapper postMapper;
    private final PostMappers.TagMapper tagMapper;
    private final PostMappers.PostTagMapper postTagMapper;
    private final UserMapper userMapper;
    private final AiMetadataService aiMetadataService;

    public PostService(PostMappers.PostMapper postMapper, PostMappers.TagMapper tagMapper,
                       PostMappers.PostTagMapper postTagMapper, UserMapper userMapper,
                       @Lazy AiMetadataService aiMetadataService) {
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
        this.userMapper = userMapper;
        this.aiMetadataService = aiMetadataService;
    }

    public PageResult<PostDtos.PostView> list(String keyword, String tag, long page, long size) {
        Set<Long> taggedIds = null;
        if (StringUtils.hasText(tag)) {
            Tag tagEntity = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tag));
            if (tagEntity == null) {
                return new PageResult<>(List.of(), page, size, 0, 0);
            }
            taggedIds = new HashSet<>(postTagMapper.selectList(new LambdaQueryWrapper<PostTag>()
                    .eq(PostTag::getTagId, tagEntity.getId())).stream().map(PostTag::getPostId).toList());
            if (taggedIds.isEmpty()) {
                return new PageResult<>(List.of(), page, size, 0, 0);
            }
        }
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>().orderByDesc(Post::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        if (taggedIds != null) {
            query.in(Post::getId, taggedIds);
        }
        IPage<Post> result = postMapper.selectPage(Page.of(page, Math.min(size, 50)), query);
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    public PostDtos.PostView get(Long id) {
        return view(requirePost(id));
    }

    @Transactional
    public PostDtos.PostView create(PostDtos.SavePostRequest request, AuthUser author) {
        Post post = new Post();
        post.setAuthorId(author.id());
        post.setCreatedAt(LocalDateTime.now());
        apply(post, request, true);
        postMapper.insert(post);
        replaceTags(post.getId(), request.tags(), "MANUAL");
        aiMetadataService.generateAsync(post.getId(), author.id());
        return view(post);
    }

    @Transactional
    public PostDtos.PostView update(Long id, PostDtos.SavePostRequest request, AuthUser author) {
        Post post = requireOwned(id, author.id());
        boolean contentChanged = !Objects.equals(post.getContent(), request.content());
        apply(post, request, contentChanged);
        postMapper.updateById(post);
        replaceTags(post.getId(), request.tags(), "MANUAL");
        if (contentChanged) {
            aiMetadataService.generateAsync(post.getId(), author.id());
        }
        return view(post);
    }

    @Transactional
    public void delete(Long id, AuthUser author) {
        requireOwned(id, author.id());
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, id));
        postMapper.deleteById(id);
    }

    public Post requirePost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        return post;
    }

    public Post requireOwned(Long id, Long authorId) {
        Post post = requirePost(id);
        if (!post.getAuthorId().equals(authorId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能操作自己的文章");
        }
        return post;
    }

    public PostDtos.AiMetadataView metadata(Long id) {
        Post post = requirePost(id);
        return new PostDtos.AiMetadataView(post.getAiMetadataStatus(), post.getAiSummary(),
                Boolean.TRUE.equals(post.getAiSummaryEdited()), post.getAiGeneratedAt(), tags(id));
    }

    public List<PostDtos.TagView> allTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName))
                .stream().map(tag -> new PostDtos.TagView(tag.getId(), tag.getName(), "AVAILABLE")).toList();
    }

    @Transactional
    public void replaceAiTags(Long postId, List<String> names) {
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getPostId, postId).eq(PostTag::getSource, "AI"));
        addTags(postId, names, "AI");
    }

    private void apply(Post post, PostDtos.SavePostRequest request, boolean triggerAi) {
        post.setTitle(request.title().trim());
        post.setSummary(request.summary() == null ? "" : request.summary().trim());
        post.setContent(request.content());
        post.setUpdatedAt(LocalDateTime.now());
        if (request.aiSummary() != null && !request.aiSummary().isBlank()) {
            post.setAiSummary(request.aiSummary().trim());
            post.setAiSummaryEdited(true);
        }
        if (triggerAi) {
            post.setAiMetadataStatus("PENDING");
        }
    }

    private void replaceTags(Long postId, List<String> names, String source) {
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getPostId, postId).eq(PostTag::getSource, source));
        addTags(postId, names, source);
    }

    private void addTags(Long postId, List<String> names, String source) {
        if (names == null) return;
        names.stream().filter(StringUtils::hasText).map(String::trim).distinct().limit(5).forEach(name -> {
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            PostTag relation = new PostTag();
            relation.setPostId(postId);
            relation.setTagId(tag.getId());
            relation.setSource(source);
            postTagMapper.insert(relation);
        });
    }

    private List<PostDtos.TagView> tags(Long postId) {
        List<PostTag> relations = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId));
        if (relations.isEmpty()) return List.of();
        Map<Long, String> sources = new HashMap<>();
        relations.forEach(relation -> sources.put(relation.getTagId(), relation.getSource()));
        return tagMapper.selectBatchIds(sources.keySet()).stream()
                .map(tag -> new PostDtos.TagView(tag.getId(), tag.getName(), sources.get(tag.getId()))).toList();
    }

    private PostDtos.PostView view(Post post) {
        User author = userMapper.selectById(post.getAuthorId());
        String displaySummary = StringUtils.hasText(post.getAiSummary()) ? post.getAiSummary() : post.getSummary();
        return new PostDtos.PostView(post.getId(), post.getAuthorId(),
                author == null ? "未知用户" : author.getUsername(), post.getTitle(), displaySummary,
                post.getContent(), post.getAiSummary(), post.getAiMetadataStatus(),
                Boolean.TRUE.equals(post.getAiSummaryEdited()), post.getAiGeneratedAt(), tags(post.getId()),
                post.getCreatedAt(), post.getUpdatedAt());
    }
}

