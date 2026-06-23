# CampusBlog AI

CampusBlog AI 是基于开源项目 `maple0511/vueblog` 重构的校园知识博客。项目保留文章创作与阅读主线，并加入 AI 摘要、自动标签、写作助手和单篇文章问答。

## 功能

- 注册、登录、JWT 认证和 BCrypt 密码。
- Markdown 文章 CRUD、搜索、标签和评论。
- 发布后异步生成 AI 摘要与标签，失败时使用人工内容。
- SSE 流式 AI 写作助手：大纲、续写、改写、标题建议。
- 基于当前文章的问答，不使用联网搜索或全站 RAG。
- AI 每日额度、超时、调用日志和敏感数据最小化。

默认 AI Provider 为阿里云百炼千问兼容接口，模型 `qwen3.7-plus`。支持深度思考模式，但前端只接收最终回答，不展示模型内部推理内容。

## 本地开发

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## Docker 部署

```bash
cp .env.example .env
# 修改 .env 中的数据库密码、JWT_SECRET、PUBLIC_ORIGIN 和可选 AI 配置
docker compose up -d --build
docker compose ps
```

默认公网端口为 `18000`。不得提交 `.env`。

启用千问：

```text
AI_ENABLED=true
AI_BASE_URL=https://ws-etymarnalsjn28ue.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
AI_API_KEY=通过服务器环境变量提供
AI_MODEL=qwen3.7-plus
AI_ENABLE_THINKING=true
```

## 验证

```bash
cd backend && mvn verify
cd frontend && npm ci && npm run build && npm test
docker compose config
```

## 文档

- [需求摘要](docs/requirements.md)
- [架构设计](docs/architecture.md)
- [数据库设计](docs/database-design.md)
- [测试报告](docs/test-report.md)
- [答辩提纲](docs/presentation-outline.md)
- [持续执行计划](task_plan.md)

## 开源说明

原始项目遵循仓库内的 MIT License。本重构保留上游历史和许可证，新增实现位于 `backend/`、`frontend/`、`docs/` 与根部署配置。
