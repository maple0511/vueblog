# Findings

## 旧仓库

- 最后提交时间为 2020-10-20，约 1673 行 Java/Vue/JS。
- Spring Boot 2.2、Java 8、Vue 2、Shiro、MD5、硬编码数据库密码与 JWT 密钥。
- CORS 允许任意来源，测试仅包含空的上下文加载。
- Dockerfile 暴露端口与实际服务端口不一致，Compose 缺少初始化、健康检查和密钥管理。

## 课程要求

- 本地模板要求需求构思、SRS、用例/时序模型、SDS、体系结构/类图/部署图、数据库设计、黑白盒测试、质量分析、答辩 PPT 和团队/个人贡献统计。
- 数据库设计与测试报告模板为空文件，需要生成有效文档。

## 实施约束

- 周期 3–5 天，团队 4 人以上。
- 产品定位为校园知识博客。
- AI 功能包括摘要、标签、写作助手和当前文章问答。
- 公网验收使用已有 Linux 服务器的 IP + 端口。

## 千问兼容接口

- AI 厂商确定为阿里云百炼千问，模型为 `qwen3.7-plus`。
- 用户提供的 Base URL 已包含 `/compatible-mode/v1`，客户端不得再次重复追加 `/v1`。
- `extra_body={"enable_thinking": true}` 在兼容 HTTP 请求中对应顶层字段 `enable_thinking`。
- 流式响应可能包含 `reasoning_content`；产品只向前端发送最终 `content`，不暴露内部思考过程。
- Spring MVC `SseEmitter` 完成时会触发 ASYNC dispatcher；若安全链只认证初始 REQUEST，会在响应已提交后产生 403 并导致 Nginx 报上游提前关闭。
