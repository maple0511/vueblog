# CampusBlog AI 架构设计

## 设计目标

- 将旧版 Spring Boot 2 + Vue 2 教学项目升级为可部署、可测试、可演示的校园知识博客。
- AI 能力围绕文章生命周期工作，不替代作者决策。
- 外部 AI 服务不可用时，文章 CRUD、搜索、评论和人工元数据保持可用。

## 系统上下文

```mermaid
flowchart LR
  U[校园创作者/读者] --> N[Nginx + Vue 3]
  N --> B[Spring Boot REST API]
  B --> M[(MySQL 8.4)]
  B --> A[阿里云百炼千问兼容服务]
```

## 后端组件

```mermaid
flowchart TB
  C[Controller] --> S[Domain Service]
  S --> R[MyBatis-Plus Mapper]
  R --> DB[(MySQL)]
  S --> P[AiProvider]
  P --> O[DashScope OpenAI Compatible API]
  F[JWT Filter] --> C
  E[Global Exception Handler] --> C
```

- `auth`：注册、登录、BCrypt、JWT。
- `auth`：注册、登录、BCrypt、JWT、用户角色、状态和兴趣标签偏好。
- `post`：文章、标签、搜索、评论、所有权检查和审核可见性。
- `admin`：管理员端帖子审核、成员启用/停用和列表筛选。
- `ai`：供应商抽象、元数据异步生成、写作/问答 SSE、额度和日志。
- `common/security`：统一响应、异常和认证上下文。

## 个性化推荐闭环

```mermaid
sequenceDiagram
  participant U as 新用户
  participant FE as Vue 前端
  participant BE as Spring Boot
  participant DB as MySQL
  U->>FE: 注册成功
  FE->>U: 跳转兴趣标签选择
  U->>FE: 选择美食/体育/学习等标签
  FE->>BE: PUT /api/users/preferences
  BE->>DB: 保存 user_tag_preferences
  FE->>BE: GET /api/recommendations/posts
  BE->>DB: 匹配已审核文章标签
  BE-->>FE: 推荐文章列表
```

推荐规则保持可解释：只按用户兴趣标签与文章标签匹配，不引入隐式画像或跨站数据。

## 部署

```mermaid
flowchart LR
  Internet -->|TCP 18000| FE[frontend container / Nginx]
  FE -->|/api| BE[backend container :8080]
  BE --> DB[(mysql container :3306)]
  BE --> AI[千问 AI Provider HTTPS]
```

公网只暴露前端端口 `18000`。后端和数据库仅通过 Compose 内部网络通信。
