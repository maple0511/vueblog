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
  B --> A[OpenAI 兼容模型服务]
```

## 后端组件

```mermaid
flowchart TB
  C[Controller] --> S[Domain Service]
  S --> R[MyBatis-Plus Mapper]
  R --> DB[(MySQL)]
  S --> P[AiProvider]
  P --> O[OpenAI Compatible API]
  F[JWT Filter] --> C
  E[Global Exception Handler] --> C
```

- `auth`：注册、登录、BCrypt、JWT。
- `post`：文章、标签、搜索、评论和所有权检查。
- `ai`：供应商抽象、元数据异步生成、写作/问答 SSE、额度和日志。
- `common/security`：统一响应、异常和认证上下文。

## 部署

```mermaid
flowchart LR
  Internet -->|TCP 18000| FE[frontend container / Nginx]
  FE -->|/api| BE[backend container :8080]
  BE --> DB[(mysql container :3306)]
  BE --> AI[AI Provider HTTPS]
```

公网只暴露前端端口 `18000`。后端和数据库仅通过 Compose 内部网络通信。
