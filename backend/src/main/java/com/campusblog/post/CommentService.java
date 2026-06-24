package com.campusblog.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusblog.auth.User;
import com.campusblog.auth.UserMapper;
import com.campusblog.common.BusinessException;
import com.campusblog.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    private final PostMappers.CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final PostService postService;

    public CommentService(PostMappers.CommentMapper commentMapper, UserMapper userMapper, PostService postService) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.postService = postService;
    }

    public List<PostDtos.CommentView> list(Long postId, AuthUser viewer) {
        postService.ensureViewable(postId, viewer);
        return commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId).orderByAsc(Comment::getCreatedAt))
                .stream().map(this::view).toList();
    }

    @Transactional
    public PostDtos.CommentView create(Long postId, PostDtos.CommentRequest request, AuthUser author) {
        postService.ensureViewable(postId, author);
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(author.id());
        comment.setContent(request.content().trim());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return view(comment);
    }

    @Transactional
    public void delete(Long id, AuthUser user) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(HttpStatus.NOT_FOUND, "评论不存在");
        Post post = postService.requirePost(comment.getPostId());
        if (!comment.getAuthorId().equals(user.id()) && !post.getAuthorId().equals(user.id())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能删除自己的评论或自己文章下的评论");
        }
        commentMapper.deleteById(id);
    }

    private PostDtos.CommentView view(Comment comment) {
        User author = userMapper.selectById(comment.getAuthorId());
        return new PostDtos.CommentView(comment.getId(), comment.getAuthorId(),
                author == null ? "未知用户" : author.getUsername(), comment.getContent(), comment.getCreatedAt());
    }
}
