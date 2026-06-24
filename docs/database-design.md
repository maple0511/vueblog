# 数据库设计报告

## E-R 模型

```mermaid
erDiagram
  USERS ||--o{ POSTS : writes
  USERS ||--o{ COMMENTS : creates
  POSTS ||--o{ COMMENTS : contains
  POSTS ||--o{ POST_TAGS : classified
  TAGS ||--o{ POST_TAGS : assigned
  USERS ||--o{ AI_REQUEST_LOG : invokes
  POSTS ||--o{ AI_REQUEST_LOG : relates
  USERS ||--o{ USER_TAG_PREFERENCES : prefers
  USERS ||--o{ POSTS : reviews

  USERS {
    bigint id PK
    varchar username UK
    varchar email UK
    varchar password_hash
    varchar role
    varchar status
    boolean profile_completed
    datetime created_at
  }
  POSTS {
    bigint id PK
    bigint author_id FK
    varchar title
    varchar summary
    longtext content
    varchar ai_summary
    varchar ai_metadata_status
    boolean ai_summary_edited
    datetime ai_generated_at
    varchar review_status
    varchar review_reason
    bigint reviewer_id FK
    datetime reviewed_at
  }
  TAGS {
    bigint id PK
    varchar name UK
  }
  POST_TAGS {
    bigint post_id FK
    bigint tag_id FK
    varchar source
  }
  COMMENTS {
    bigint id PK
    bigint post_id FK
    bigint author_id FK
    varchar content
  }
  USER_TAG_PREFERENCES {
    bigint user_id FK
    varchar tag_name
    datetime created_at
  }
  AI_REQUEST_LOG {
    bigint id PK
    bigint user_id FK
    bigint post_id FK
    varchar feature
    varchar status
    bigint latency_ms
    int prompt_tokens
    int completion_tokens
  }
```

`ai_request_log` 不保存文章正文、问题、提示词、回答、令牌或密码。

## 关键扩展说明

- `users.role`：`ADMIN` 或 `USER`，用于管理员后台访问控制。迁移时会将最早注册用户设置为管理员。
- `users.status`：`ACTIVE` 或 `DISABLED`，停用成员无法登录。
- `users.profile_completed`：标记是否完成注册后的兴趣标签选择。
- `user_tag_preferences`：保存用户显式选择的兴趣标签，推荐页只基于这些标签匹配文章。
- `posts.review_status`：`APPROVED`、`REJECTED`、`HIDDEN`。默认 `APPROVED`，保证原发布体验不被审核流程阻塞。
