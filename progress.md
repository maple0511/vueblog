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
- 服务器端 Maven Central 速度过慢，增加 `Dockerfile.runtime` 与 Compose 覆盖文件，支持部署本地已通过测试的 JAR。
- 后端镜像安装 `curl`，使 Actuator 健康检查在容器内可执行。
- 公网注册、发布和评论冒烟测试通过；测试同时发现分页拦截器缺失和 AI 异步事务竞态。
- 增加 MyBatis-Plus 分页插件、事务提交后 AI 元数据事件及对应后端回归测试。
- 修复后重新部署，公网搜索返回正确总数；AI 禁用时状态转为 `FAILED` 且人工摘要保留。
- 公网入口 `http://1.94.218.30:18000` 返回 HTTP 200，MySQL、后端与前端容器运行正常。
- 修正前端健康检查为 IPv4 回环地址，避免 Alpine 容器内 `localhost` 解析差异。
- 服务器重复前端构建因资源紧张被中止；改用 Compose 健康检查覆盖并复用现有镜像。
- AI Provider 默认配置切换为阿里云百炼千问 `qwen3.7-plus`，支持 `enable_thinking`。
- 兼容端点拼接同时支持 Base URL 已包含或未包含 `/v1` 的情况。
- 增加千问非流式请求体和 SSE 流式解析测试，最终输出忽略 `reasoning_content`。
- 后端共 5 个测试通过，Compose 中千问端点、模型和深度思考配置解析正确。
- README 与架构文档更新为阿里云百炼千问部署说明。
- 服务器密钥已通过受保护的 `.env` 注入，未写入仓库；千问摘要与自动标签真实调用成功。
- 首次写作 SSE 返回内容后在异步派发阶段被安全链拒绝，已增加 ASYNC/ERROR dispatcher 放行修复。
- SSE 安全修复提交 `8155301` 已推送并部署，服务器三个容器保持健康。
- 千问真实验收通过：AI 摘要 121 字、3 个自动标签、标题建议流式完成、文章问答流式完成。
- 无文章依据的收费问题严格返回“该文章未提供相关信息”。
- `ai_request_log` 中千问 METADATA、WRITING、QUESTION 均记录成功，仅保存特征、状态、模型、耗时和 token 统计字段。

## 2026-06-24

- 开始第 7 阶段：管理员审核管理与新用户兴趣推荐。
- 已确认当前分支为 `refactor/campus-blog`，工作区干净；继续使用非主分支提交。
- 设计扩展点：新增用户角色/状态/兴趣偏好、帖子审核状态，前端新增 `/admin`、`/onboarding`、`/recommendations`。
- 新增 Flyway V2：用户角色/状态/兴趣完成状态、用户兴趣标签表、帖子审核状态/原因/审核人/审核时间。
- 后端新增管理员 API：帖子审核与成员启用/停用；首个注册用户自动成为 `ADMIN`。
- 后端新增用户兴趣偏好 API 和推荐 API：按显式标签匹配已审核文章。
- 前端新增注册后兴趣选择页、推荐页和管理员后台；导航根据登录状态和角色展示入口。
- 修复标签校验默认提示，单个标签超长时返回“每个标签不能超过20个字符”。
- 验证通过：后端 `mvn verify` 6 个测试零失败；前端 `npm test` 1 个测试通过；前端 `npm run build` 通过；`docker compose config` 通过。
