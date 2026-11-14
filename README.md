# AIMS · AI 多平台文案与工作流系统

基于 Spring Boot 3 + Vue 3 的 AI 自媒体文案系统，支持**文案生成**、**文章美化**、**文章续写**、**新闻热点管理**、**Markdown 编辑器**、**发布历史**与**用户管理**。AI 能力统一由**通义千问（阿里云百炼 DashScope）大模型**提供（生成用 `qwen-plus`、新闻摘要用 `qwen-turbo`），并内置错误日志落库与定时清理任务，便于排查问题与维护。

## 技术栈

### 后端

- Spring Boot 3.2
- MyBatis + MyBatis-Plus
- MySQL 5.7+（库名 `basesys`）
- Druid 连接池
- Jackson / 流式 SSE
- 阿里云百炼 DashScope SDK（通义千问：`qwen-plus` 生成、`qwen-turbo` 摘要）
- HtmlUnit + Jsoup（新浪新闻热点爬虫）
- Hutool、Lombok、Spring `@Scheduled` 定时任务

### 前端

- Vue 3 + Element Plus
- Vue Router、Pinia
- Axios、ECharts（仪表盘）

> **说明**：本仓库为 **Java (Spring Boot) + Vue** 单体应用，构建与运行以 Maven / JDK 17+ 为准。AI 能力由**通义千问（DashScope）大模型 API** 提供，已替代原先的 Coze 云端工作流，不包含 Python 入口或 `.coze` 运行时配置。

## 功能特性

### 文案生成

- 输入主题，勾选平台（抖音 / 小红书 / 视频号 / 今日头条），调用通义千问大模型（`qwen-plus`）生成多平台文案。
- 支持 **目标读者**（大众 / 专业人士 / 学生 / 企业）、流式输出。
- 输出格式：每个平台一段，段首单独一行以 `【平台名】` 作为标题。

### 文章美化

- 粘贴原文，选择美化类型（整体优化 / 增强文采 / 精简文字 / 调整语气）与目标风格，调用通义千问大模型流式返回结果。

### 文章续写

- 输入文章开头与续写提示，选择**续写长度**（短篇 / 中等 / 长篇）、**续写风格**（保持原风格 / 更正式 / 更生动），调用通义千问大模型流式返回续写内容。

### 新闻热点管理

- 基于 **HtmlUnit + Jsoup** 爬取新浪新闻热点（国内 / 国际 / 社会 / 体育 / 财经 / 娱乐 / 科技），分页查询、按标题/类型筛选。
- **AI 摘要**：调用通义千问 `qwen-turbo` 对新闻内容生成摘要，摘要结果缓存到 `article_news.summary` 列，避免重复调用大模型（token 降本）。
- **定时清理**：通过 `NewsCleanupTask` 定时清理过时热点，保留天数与执行 cron 由 `application.yml` 的 `news.cleanup` 配置（默认保留 3 天、每天凌晨 3 点执行）。

### Markdown 编辑器

- 在线编辑 Markdown 文案，支持保存/更新草稿（首次保存插入、再次保存更新 `ai_markdown_copy` 表）。

### 发布历史与平台管理

- 维护发布平台（`article_platform`），记录历史发布记录（`publish_history`）。

### 用户与权限

- 管理员（`admins`）与普通用户（`users`）双表，JWT 登录认证，支持用户管理（增删改查、启禁用）。

### 仪表盘

- 基于 ECharts 的统计仪表盘，可视化文案生成、热点等数据。

### 私有知识库问答（RAG）

- 独立服务目录：`rag-service/`（FastAPI，默认端口 `8001`）。
- 最小链路：上传文本 → 关键词检索 → 拼 Prompt → 通义千问（Qwen）→ SSE 流式返回答案。
- 前端入口：侧栏「私有知识库」→ `/knowledge`（Vite 代理 `/rag` → `8001`）。
- 启动见 [rag-service/README.md](rag-service/README.md)，需配置 `DASHSCOPE_API_KEY`。

### 错误日志与排查

- **MySQL 错误日志表**：`basesys.ai_error_log`，记录各模块（generate / polish / continue / qwen / auth 等）出错时的**模块**、**阶段**、错误摘要、堆栈、请求上下文。
- **错误返回提示**：接口/前端返回的错误文案包含「[模块] 阶段「xxx」失败：原因。错误已记录至系统日志。」便于定位出错的程序与步骤。
- **错误日志 API**：
  - `GET /api/error-log/list?module=&page=1&size=20`：分页查询，可选按 `module` 筛选。
  - `GET /api/error-log/{id}`：单条详情（含完整 `error_detail` 堆栈）。
  - `DELETE /api/error-log/{id}`：删除单条。

## 项目结构

```
aims/
├── src/main/
│   ├── java/com/product/
│   │   ├── controller/     # AiController、ArticleNewsController、ArticlePlatformController、AuthController、
│   │   │                   # DashboardController、ErrorLogController、HistoryController、MarkdownCopyController、
│   │   │                   # PublishHistoryController、UploadController、UserManagementController
│   │   ├── service/        # GenerationService、PolishService、ContinuationService、ArticleNewsService、
│   │   │                   # QwenWorkflowClient、ErrorLogService、AuthService、UserManagementService 等
│   │   ├── mapper/         # MyBatis Mapper 接口
│   │   ├── entity/         # GenerationTask、PolishTask、ContinuationTask、ArticleNews、ArticlePlatform、
│   │   │                   # Admin、AppUser、MarkdownCopy、PublishHistory、ErrorLog 等
│   │   ├── spider/         # SinaHotNewsSpider（新浪新闻热点爬虫）
│   │   ├── task/           # NewsCleanupTask（热点定时清理）
│   │   ├── util/           # JwtUtil 等
│   │   └── config/ dto/ vo/ param/ common/ constant/
│   └── resources/
│       ├── application.yml  # 含 spring.sql.init 自动初始化配置
│       ├── schema.sql       # 权威建库脚本（11 张表，IF NOT EXISTS 幂等）
│       ├── mapper/          # MyBatis XML
│       └── db/              # 增量迁移脚本（如 init.sql）
├── admin-ui/              # Vue3 管理前端（Vite）
├── doc/                   # 文档（论文规范、格式说明等）
└── README.md
```

## 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 5.7+
- Node.js 16+

### 2. 数据库初始化

- 使用库名 **basesys**，`src/main/resources/schema.sql` 为**权威建库脚本**，包含全部 **11 张表**，统一使用 `CREATE TABLE IF NOT EXISTS`，可重复执行、幂等：

  | 表 | 说明 |
  | --- | --- |
  | `admins` | 管理员表（含默认账号 `admin/admin123`） |
  | `users` | 普通用户表 |
  | `ai_generation` | 文案生成 |
  | `ai_polish` | 文章美化 |
  | `ai_continuation` | 文章续写 |
  | `ai_task` | AI 任务记录 |
  | `ai_error_log` | 错误日志 |
  | `ai_markdown_copy` | Markdown 文案草稿 |
  | `article_news` | 新闻热点（含 `summary` 摘要缓存列） |
  | `article_platform` | 发布平台 |
  | `publish_history` | 历史发布记录 |

- **自动初始化**：`application.yml` 已开启 `spring.sql.init.mode: always`，后端启动时会自动执行 `classpath:schema.sql`（`IF NOT EXISTS` 保证幂等，重复启动安全）。
- 若库已存在且缺少表或列，可**一次性执行** `src/main/resources/db/init.sql`：通过存储过程判断列是否存在，为 `ai_generation` 补充各平台文案列（`douyin_copy` / `xiaohongshu_copy` / `video_account_copy` / `toutiao_copy`），为 `ai_polish`、`ai_continuation` 补充 `platform` 列，并将旧 `admins` 数据迁移至 `users`。可重复执行，已存在则跳过。

### 3. 通义千问（DashScope）配置

1. 在阿里云百炼（Model Studio）控制台开通 DashScope 并获取 API Key。
2. API Key 已按要求直接配置在代码中（`QwenWorkflowClient.java` 的常量 `API_KEY`）。
3. 模型名称可在 `application.yml` 中配置：

   ```yaml
   qwen:
     model: qwen-plus          # 文案生成/美化/续写使用的模型
     summary-model: qwen-turbo # 新闻摘要使用的轻量模型（token 降本）
   ```

### 4. 后端启动

```bash
cd aims
mvn spring-boot:run
```

### 5. 前端启动

```bash
cd admin-ui
npm install
npm run dev
```

### 6. 访问

- 前端：<http://localhost:3000>
- 后端 API：<http://localhost:8080/api>
- Druid 监控：<http://localhost:8080/api/druid>
- 错误日志列表（示例）：<http://localhost:8080/api/error-log/list>

## 测试账号

- 管理员：`admin` / `admin123`（由 `schema.sql` 种子数据自动创建）。
- 普通用户：可在「用户管理」中创建，或注册时使用默认密码（`application.yml` 中 `app.default-password`，默认为 `root`）。

## 部署说明

- 后端：`mvn clean package` 后运行 `target/*.jar`。
- 前端：`npm run build`，将 `dist` 部署至 Web 服务器。
- 生产环境请配置 HTTPS、修改 JWT 密钥与默认密码、限制 CORS 与敏感接口。

## 许可证

MIT License
