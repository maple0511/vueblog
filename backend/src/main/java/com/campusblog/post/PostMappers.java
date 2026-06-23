package com.campusblog.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

public final class PostMappers {
    private PostMappers() {
    }

    @Mapper
    public interface PostMapper extends BaseMapper<Post> {}
    @Mapper
    public interface TagMapper extends BaseMapper<Tag> {}
    @Mapper
    public interface PostTagMapper extends BaseMapper<PostTag> {}
    @Mapper
    public interface CommentMapper extends BaseMapper<Comment> {}
}
