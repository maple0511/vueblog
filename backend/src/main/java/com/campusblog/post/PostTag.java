package com.campusblog.post;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("post_tags")
public class PostTag {
    private Long postId;
    private Long tagId;
    private String source;
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

