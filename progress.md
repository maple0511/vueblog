# Progress

## 2026-06-23

- 阅读课程 Word、Excel、PowerPoint 模板并提取交付要求。
- 分析上游仓库的前后端、数据库、安全、部署与测试现状。
- 克隆 `maple0511/vueblog`，创建 `refactor/campus-blog` 分支。
- 初始化 Planning with Files 三份持久文件。
- 创建 Spring Boot 3 后端、Flyway 数据库、JWT 安全、文章/标签/评论和 AI 接口初版。
- 用户提供服务器 `1.94.218.30` 与 SSH 端口 `2000`；认证凭据不写入仓库。
- 确认所有提交使用 `refactor/campus-blog`，不修改默认主分支。
- 本机 Docker daemon 未运行，后端验证切换为临时 Maven 运行时。
- 后端首次编译发现 Java 21 虚拟线程 API，已改为 Java 17 兼容的固定线程池。
- 修复 MyBatis 将 `AiProvider` 误识别为 Mapper 的扫描边界问题。
- 完成 Vue 3 前端页面、AI SSE 客户端、Markdown 清洗和响应式样式初版。
- 修复前端 TypeScript 路径映射和第三方类型声明检查配置。
- 后端测试通过；前端生产构建与 Vitest 通过。
- 移除旧版 Vue 2/Spring Boot 2 源码树，避免遗留明文密码和双实现混淆；历史仍保留在 Git 中。
- `mvn verify`、`npm run build`、`npm test` 和 `docker compose config` 均通过。
- 提交 `e20c5e8` 已推送至 GitHub `refactor/campus-blog`，默认分支未修改。
- 远程检查确认 SSH 使用 2000；80/443/8080 已被既有服务占用，选择空闲端口 18000 部署。
