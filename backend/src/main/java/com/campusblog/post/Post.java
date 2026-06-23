package com.campusblog.post;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("posts")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private String title;
    private String summary;
    private String content;
    private String aiSummary;
    private String aiMetadataStatus;
    private Boolean aiSummaryEdited;
    private LocalDateTime aiGeneratedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getAiMetadataStatus() { return aiMetadataStatus; }
    public void setAiMetadataStatus(String aiMetadataStatus) { this.aiMetadataStatus = aiMetadataStatus; }
    public Boolean getAiSummaryEdited() { return aiSummaryEdited; }
    public void setAiSummaryEdited(Boolean aiSummaryEdited) { this.aiSummaryEdited = aiSummaryEdited; }
    public LocalDateTime getAiGeneratedAt() { return aiGeneratedAt; }
    public void setAiGeneratedAt(LocalDateTime aiGeneratedAt) { this.aiGeneratedAt = aiGeneratedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

