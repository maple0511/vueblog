# CampusBlog AI 软件测试报告

## 1. 测试概述

本报告针对 `maple0511/vueblog` 重构后的 CampusBlog AI 校园知识博客进行黑盒测试、白盒测试和部署验收测试。测试对象包括：

- 用户注册、登录、JWT 认证、角色和成员状态。
- 文章创建、编辑、搜索、标签筛选、详情、评论和所有权校验。
- 管理员帖子审核、成员启用/停用。
- 新用户兴趣标签选择和推荐页。
- AI 文章摘要、自动标签、AI 写作助手、单篇文章问答。
- Docker Compose 部署、Flyway 迁移、容器健康检查和公网入口。

测试时间：2026-06-25  
公网地址：`http://1.94.218.30:18000`  
测试分支：`refactor/campus-blog`

## 2. 测试环境

| 项目 | 环境 |
|---|---|
| 本地系统 | macOS，Asia/Shanghai |
| 后端 | Java 17、Spring Boot 3.5.15、MyBatis-Plus、JUnit、MockMvc、JaCoCo |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vitest |
| 数据库 | 本地测试 H2 MySQL 模式；公网 MySQL 8.4 |
| 部署 | Docker Compose、Nginx、后端 runtime JAR、前端 runtime dist |
| AI | 阿里云百炼千问 OpenAI 兼容接口，模型 `qwen3.7-plus` |

执行命令：

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 17) /opt/homebrew/bin/mvn verify
cd frontend && npm test
cd frontend && npm run build
docker compose -f docker-compose.yml -f docker-compose.runtime.yml config
```

## 3. 测试策略

### 3.1 黑盒测试策略

黑盒测试从用户可见行为出发，不依赖内部实现，按业务闭环设计用例：

1. 认证闭环：注册、重复注册、错误密码、当前用户信息、未登录保护。
2. 内容闭环：发布文章、搜索、标签筛选、详情、评论。
3. 推荐闭环：注册后选择兴趣标签，并在推荐页看到匹配文章。
4. 管理闭环：管理员登录、查看帖子、隐藏和恢复帖子。
5. AI 闭环：自动摘要/标签、写作助手 SSE、单篇文章问答。
6. 安全闭环：非管理员访问后台失败，隐藏内容不出现在公开列表。

### 3.2 白盒测试策略

白盒测试基于代码结构、条件分支和异常路径设计，重点覆盖：

- `AuthService`：唯一性检查、BCrypt 密码匹配、停用用户、JWT 用户视图。
- `PostService`：搜索条件、审核状态过滤、作者所有权、标签替换、推荐匹配。
- `CommentService`：评论作者删除、文章作者删除、陌生用户禁止删除。
- `AdminService`：管理员成员状态修改、审核状态合法性。
- `AiUsageService`：每日限流路径。
- `OpenAiCompatibleProvider`：DashScope 兼容路径、`enable_thinking` 请求体、忽略 `reasoning_content`。
- 前端 `streamPost`：SSE chunk 事件、error 事件和 Authorization Header。

## 4. 黑盒测试用例与结果

公网黑盒测试使用一次性测试用户执行，证据文件为 [`blackbox-2026-06-25.json`](test-evidence/blackbox-2026-06-25.json)。共执行 22 项，全部通过。

| 编号 | 用例 | 输入/操作 | 预期结果 | 实际结果 |
|---|---|---|---|---|
| B01 | 首页可访问 | GET `/` | HTTP 200 且加载 CampusBlog 页面 | 通过 |
| B02 | 用户注册成功 | 新用户名、邮箱、密码 | 201，返回 JWT 和用户信息 | 通过 |
| B03 | 重复注册被拒绝 | 同一用户名/邮箱再次注册 | 409，提示用户名或邮箱已被使用 | 通过 |
| B04 | 错误密码登录失败 | 正确账号、错误密码 | 401 | 通过 |
| B05 | 当前用户信息可获取 | 携带 JWT 调用 `/api/auth/me` | 200，返回邮箱和用户信息 | 通过 |
| B06 | 未登录发布文章被拒绝 | 不带 JWT 调用 POST `/api/posts` | 401 | 通过 |
| B07 | 兴趣标签保存成功 | 选择“美食、学习” | 200，返回已选标签 | 通过 |
| B08 | 文章发布成功 | 标题、摘要、Markdown 正文、标签 | 201，返回文章 ID | 通过 |
| B09 | 关键词搜索命中 | 用测试唯一标识搜索 | 文章列表至少命中 1 条 | 通过 |
| B10 | 标签筛选可用 | 按“美食”标签筛选 | 返回包含目标文章的列表 | 通过 |
| B11 | 推荐页包含兴趣匹配文章 | 保存兴趣后访问推荐页 API | 返回同标签文章 | 通过 |
| B12 | 评论发布成功 | 登录用户发布评论 | 201，返回评论内容 | 通过 |
| B13 | 评论列表可见 | GET 评论列表 | 能看到刚发布评论 | 通过 |
| B14 | 指定管理员可登录 | `admin/admin` | 200，角色为 `ADMIN` | 通过 |
| B15 | 管理员帖子列表可查 | 管理员查询帖子 | 200，命中目标文章 | 通过 |
| B16 | 管理员可隐藏帖子 | 设置 `reviewStatus=HIDDEN` | 返回隐藏状态 | 通过 |
| B17 | 隐藏帖子不出现在公开列表 | 公开搜索隐藏帖子 | 总数为 0 | 通过 |
| B18 | 管理员可恢复帖子 | 设置 `reviewStatus=APPROVED` | 返回通过状态 | 通过 |
| B19 | 作者可触发 AI 元数据重生成 | POST 重新生成接口 | 202/成功接受 | 通过 |
| B20 | AI 元数据最终完成 | 轮询 AI 状态 | 状态为 `READY` | 通过 |
| B21 | AI 写作助手 SSE 返回内容 | 标题建议 | SSE 返回内容和 done 事件 | 通过 |
| B22 | 文章问答缺少依据时拒绝推测 | 问“学费金额” | 返回“该文章未提供相关信息” | 通过 |

黑盒结论：系统主业务流、权限控制、管理员审核、推荐闭环和 AI 功能在公网环境均可用；隐藏内容不会被公开列表检索到。

## 5. 白盒测试设计与结果

白盒测试证据文件为 [`whitebox-2026-06-25.json`](test-evidence/whitebox-2026-06-25.json)。

### 5.1 后端自动化测试

| 测试类 | 测试数 | 失败 | 错误 | 覆盖重点 |
|---|---:|---:|---:|---|
| `AuthFlowTest` | 4 | 0 | 0 | 注册、当前用户、重复注册、错误登录、停用用户 |
| `PostFlowTest` | 6 | 0 | 0 | 文章、AI 降级、管理员审核、推荐、越权、评论、校验、AI 限流 |
| `OpenAiCompatibleProviderTest` | 2 | 0 | 0 | 千问兼容请求体、流式响应解析、忽略推理内容 |
| 合计 | 12 | 0 | 0 | 后端关键业务与异常分支 |

### 5.2 前端自动化测试

| 测试文件 | 测试数 | 结果 | 覆盖重点 |
|---|---:|---|---|
| `auth.test.ts` | 1 | 通过 | Pinia 登录态清理 |
| `api.test.ts` | 2 | 通过 | SSE chunk 解析、SSE error 抛错、JWT Header |
| 合计 | 3 | 通过 | 前端状态和 AI 流式基础行为 |

### 5.3 覆盖率

JaCoCo 统计结果如下：

| 指标 | 覆盖 | 总量 | 覆盖率 |
|---|---:|---:|---:|
| 指令覆盖 | 2905 | 3920 | 74.11% |
| 分支覆盖 | 75 | 166 | 45.18% |
| 行覆盖 | 587 | 754 | 77.85% |
| 方法覆盖 | 244 | 290 | 84.14% |
| 圈复杂度覆盖 | 258 | 374 | 68.98% |

按包统计行覆盖率：

| 包 | 行覆盖率 | 说明 |
|---|---:|---|
| `com.campusblog.auth` | 98.11% | 认证主路径和异常路径覆盖充分 |
| `com.campusblog.security` | 91.55% | JWT 解析和认证上下文覆盖充分 |
| `com.campusblog.post` | 81.75% | 文章、标签、评论和推荐覆盖较充分 |
| `com.campusblog.common` | 80.00% | 统一异常与响应覆盖较充分 |
| `com.campusblog.ai` | 66.02% | Provider、限流和部分 AI 元数据路径已覆盖 |
| `com.campusblog.admin` | 50.00% | 管理员主路径已覆盖，非法状态分支仍可继续增强 |
| `com.campusblog.config` | 100.00% | 分页插件配置覆盖 |

行覆盖率 77.85%，满足项目计划中“不低于 60%”的标准。分支覆盖率相对较低，主要原因是 DTO、异常兜底和 AI 失败细分路径较多；本轮已覆盖核心安全分支和主要业务异常分支。

### 5.4 白盒路径说明

| 编号 | 模块 | 被测路径 | 断言 |
|---|---|---|---|
| W01 | `AuthService.register` | 用户名/邮箱唯一性失败 | 返回 409 |
| W02 | `AuthService.login` | 密码错误 | 返回 401 |
| W03 | `AuthService.login` | 用户状态为 `DISABLED` | 返回 403 |
| W04 | `PostService.update` | 非作者修改文章 | 返回 403 |
| W05 | `CommentService.delete` | 非评论作者且非文章作者删除评论 | 返回 403 |
| W06 | `PostDtos.SavePostRequest` | 单个标签超过 20 字符 | 返回“每个标签不能超过20个字符” |
| W07 | `UserPreferenceService.save` | 兴趣标签超过 10 个 | 返回“最多只能选择10个兴趣标签” |
| W08 | `SecurityConfig` | 普通用户访问 `/api/admin/**` | 返回 403 |
| W09 | `AiUsageService.assertWithinLimit` | 当日调用达到 50 次 | 返回 429 |
| W10 | `OpenAiCompatibleProvider.stream` | SSE 中包含 `reasoning_content` | 不输出推理内容，只输出正文 |
| W11 | `PostService.review` | 管理员隐藏帖子 | 公开列表过滤该帖子 |
| W12 | 前端 `streamPost` | chunk 与 error 事件 | chunk 顺序拼接，error 转异常 |

## 6. 部署与运行验收

| 项目 | 结果 |
|---|---|
| Compose runtime 配置 | `docker compose -f docker-compose.yml -f docker-compose.runtime.yml config` 通过 |
| 公网首页 | HTTP 200 |
| 后端容器 | healthy |
| 前端容器 | healthy |
| MySQL 容器 | healthy |
| Flyway | 已执行 V1、V2 迁移 |
| 管理员账号 | `admin/admin` 可登录，角色为 `ADMIN` |
| 密钥处理 | `.env` 保留在服务器，API Key 未进入仓库 |

## 7. 缺陷记录与处理

| 编号 | 缺陷/风险 | 发现方式 | 处理结果 |
|---|---|---|---|
| D01 | 默认校验提示“个数必须在0和20之间”不易理解 | 手工测试标签长度 | 改为“每个标签不能超过20个字符” |
| D02 | 早期分页未生效导致搜索总数错误 | 公网冒烟 | 注册 MyBatis-Plus 分页拦截器并回归测试 |
| D03 | AI 元数据异步任务在事务提交前读取不到文章 | 公网冒烟 | 改为事务提交后发布事件并消费 |
| D04 | SSE 完成后的 ASYNC dispatcher 被安全链拦截 | 千问真实测试 | 放行 ASYNC/ERROR dispatcher |
| D05 | 服务器低内存下前端 Node 构建耗时异常 | 部署测试 | 增加前端 runtime 镜像，服务器只接收本地 dist |

## 8. 风险与改进建议

- 管理员密码 `admin/admin` 仅适合课程演示，公网长期运行应改为强密码或首次登录强制修改。
- AI 黑盒测试会消耗真实模型额度，CI 中应继续使用模拟 AI 服务。
- 分支覆盖率 45.18%，后续可增加 AI 超时、Provider HTTP 429、非法管理员审核状态、数据库异常等分支测试。
- 前端当前以单元测试和构建为主，若时间允许可补充 Playwright 端到端测试。

## 9. 测试结论

CampusBlog AI 已完成完整黑盒与白盒测试。测试结果表明：

1. 核心业务闭环可用：注册、登录、发布、搜索、标签、评论、推荐、管理员审核均通过。
2. AI 创新闭环可用：AI 元数据生成、写作助手、文章问答均在公网真实环境通过。
3. 安全约束有效：未登录保护、越权保护、普通用户后台访问限制、隐藏帖子公开过滤均通过。
4. 自动化测试稳定：后端 12 个测试、前端 3 个测试全部通过。
5. 覆盖率达标：后端行覆盖率 77.85%，超过 60% 质量标准。

结论：系统达到课程项目测试验收要求，可用于公网演示和答辩材料引用。
