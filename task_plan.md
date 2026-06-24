# CampusBlog AI 重构执行计划

## 目标

将原 VueBlog 重构为可通过 Docker Compose 部署的 AI 校园知识博客，完成认证、文章、标签、搜索、评论、AI 摘要/标签、AI 写作助手、单篇文章问答、测试与课程材料。

## 阶段

| 阶段 | 状态 | 验收标准 |
|---|---|---|
| 1. 仓库与规划初始化 | complete | 新分支及三份规划文件存在 |
| 2. 后端与数据库 | complete | Spring Boot 构建通过，核心 API 与 AI 抽象完成 |
| 3. Vue 3 前端 | complete | 构建通过，核心业务及 AI 交互可用 |
| 4. 部署与自动化测试 | complete | Compose 配置有效，前后端测试与公网冒烟通过 |
| 5. 课程文档与验收 | complete | UML/需求/设计/数据库/测试与答辩源材料齐全 |
| 6. 千问 Provider 接入 | complete | 千问配置、请求体、流式解析测试及真实公网调用全部通过 |
| 7. 管理员审核与个性化推荐 | complete | 管理员可审核帖子/管理成员，新用户注册后可选择兴趣标签并获得推荐文章 |

## 关键决策

- 后端使用 Java 17、Spring Boot 3.5、MyBatis-Plus、Flyway、Spring Security。
- 前端使用 Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus。
- AI 通过 OpenAI 兼容接口接入；未配置或失败时不阻止文章保存。
- 默认 AI 服务为阿里云百炼千问 `qwen3.7-plus`，启用深度思考但不向前端暴露推理内容。
- AI 写作与问答使用 POST + SSE，避免在 URL 中暴露令牌和输入。
- 单篇问答不使用向量数据库，仅使用当前文章上下文。
- 新代码位于 `backend/`、`frontend/`、`deploy/`、`docs/`；旧项目保留作重构对照。
- 管理员端采用最小可演示 RBAC：首个注册用户自动成为 `ADMIN`，帖子默认 `APPROVED` 保持原发布体验，管理员可驳回/隐藏。
- 个性化推荐不引入复杂推荐系统，先使用用户兴趣标签与文章标签匹配，作为课程创新点的可解释推荐页。

## 错误记录

| 错误 | 尝试 | 处理 |
|---|---:|---|
| 本机 Docker daemon 未启动 | 1 | 改用临时 Maven/Node 完成本地构建，Compose 在远程服务器验证 |
| Java 17 不支持虚拟线程执行器 | 1 | SSE 工作线程改为固定线程池，保持 Java 17 基线 |
| MyBatis 广泛扫描误注册 AiProvider | 1 | 移除全包扫描，仅给真实 Mapper 添加 `@Mapper` |
| 前端 TypeScript 未配置 `@` 路径且检查第三方声明失败 | 1 | 增加 paths/baseUrl，跳过第三方 `.d.ts`，CI 固定 Node 22 |
| Vite 配置类型不识别 Vitest `test` 字段 | 1 | 使用 `vitest/config` 的 `defineConfig` |
| `vue-tsc -b` 向源码目录生成 JS 副本 | 1 | `tsconfig.app.json` 启用 `noEmit` 并清理生成文件 |
| 清理生成文件时重复了 `frontend/` 路径 | 1 | 在前端工作目录改用 `src/...` 路径 |
| SSE 固定线程池阻止 Maven 测试进程退出 | 1 | 使用具名守护线程，保持容器和测试可停止 |
| Maven verify 在仓库根目录执行 | 1 | 改在 `backend/` 执行 |
| SSH 默认 22 端口被远程关闭 | 1 | 将用户提供的 2000 解释为 SSH 端口并改用 `ssh -p 2000` |
| 本机 Maven 命令不在 PATH | 1 | 使用 Homebrew 安装 Maven 本体，并显式设置 JDK 17 后验证 |
| Homebrew Maven 安装卡在 OpenJDK 依赖下载 | 1 | 本机已有 JDK 17，改用 `brew install maven --ignore-dependencies` 安装 Maven 本体 |
| 公网冒烟脚本将首页 HTML 当 JSON 解析 | 1 | 将首页检查改为只验证 HTTP 200，其余 API 继续解析 JSON |
| 服务器完整 Git clone 长时间停滞 | 1 | 中止后改用部署所需的 `--depth 1` 单分支快照 |
| 服务器 Maven Central 下载依赖长时间停滞 | 1 | 增加预构建 JAR 的运行时镜像模式，由本地验证后上传 |
| 公网验收时分页总数为 0 且关键词条件未生效 | 1 | 注册 MyBatis-Plus 分页拦截器并增加搜索回归测试 |
| 事务提交前异步任务读取不到新文章，状态停留 PENDING | 1 | 改为事务提交后发布并消费 AI 元数据事件 |
| 前端健康检查使用 localhost 时解析到未监听地址 | 1 | 固定使用 `127.0.0.1/healthz` |
| 低内存服务器重新执行前端 Node 构建耗时异常 | 1 | 中止重复构建，由 Compose 覆盖健康检查并复用已验证镜像 |
| SSE 完成后的异步二次派发被 Spring Security 拒绝 | 1 | 显式放行 ASYNC 和 ERROR dispatcher，原始 API 请求仍要求 JWT |
