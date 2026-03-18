# AIMS · AI 多平台文案与工作流系统

基于 Spring Boot 3 + Vue 3 的 AI 文案系统，支持**文案生成**、**文章美化**、**文章续写**，统一调用 Coze 云端工作流（stream_run），并提供错误日志落库与查询，便于排查问题。

## 技术栈

### 后端

- Spring Boot 3.2
- MyBatis 3.0
- MySQL 5.7+（库名 `basesys`）
- Druid 连接池
- Jackson / 流式 SSE

### 前端

- Vue 3 + Element Plus
- Vue Router、Pinia
- Axios、ECharts（仪表盘）

> **说明**：本仓库为 **Java (Spring Boot) + Vue** 单体应用，构建与运行以 Maven / JDK 17+ 为准。与 Coze 的关系仅为**调用其云端工作流 API**，不包含 Python 入口或 `.coze` 运行时配置。

## 功能特性

### 文案生成

- 输入主题，勾选平台（抖音 / 小红书 / 视频号 / 今日头条），调用 Coze 工作流生成多平台文案。
- 支持 **目标读者**（大众 / 专业人士 / 学生 / 企业）、流式输出。
- 请求体：`topic`、`platform_select`、`operations: ["generate"]`、可选 `target_audience`。

### 文章美化

- 粘贴原文，选择美化类型（简化 / 润色 / 语法修正 / 改写）与目标风格，调用 Coze **beautify** 流式返回结果。
- 请求体：`operations: ["beautify"]`、`article_content`、`beautify_target` 等。

### 文章续写

- 输入文章开头与续写提示，选择**续写长度**（短篇 / 中等 / 长篇）、**续写风格**（保持原风格 / 更正式 / 更生动），调用 Coze **extend** 流式返回续写内容。
- 请求体：`operations: ["extend"]`、`article_content`、`extend_length`、`extend_style` 等。

### 错误日志与排查

- **MySQL 错误日志表**：`basesys.ai_error_log`，记录各模块（generate / polish / continue / coze / auth）出错时的**模块**、**阶段**、错误摘要、堆栈、请求上下文。
- **错误返回提示**：接口/前端返回的错误文案包含「[模块] 阶段「xxx」失败：原因。错误已记录至系统日志。」便于定位出错的程序与步骤。
- **错误日志 API**：
  - `GET /api/error-log/list?module=&page=1&size=20`：分页查询，可选按 `module` 筛选。
  - `GET /api/error-log/{id}`：单条详情（含完整 `error_detail` 堆栈）。
  - `DELETE /api/error-log/{id}`：删除单条。

详见 [doc/coze-useage.md](doc/coze-useage.md)（Coze 入参说明）、[doc/toutiao-issue-analysis.md](doc/toutiao-issue-analysis.md)（今日头条排查）。

## 项目结构

```
aims/
├── src/main/
│   ├── java/com/product/
│   │   ├── controller/     # AiController、ErrorLogController、AuthController 等
│   │   ├── service/        # GenerationService、PolishService、ContinuationService、CozeWorkflowClient、ErrorLogService
│   │   ├── mapper/         # GenerationMapper、PolishMapper、ContinuationMapper、ErrorLogMapper
│   │   └── entity/        # GenerationTask、PolishTask、ContinuationTask、ErrorLog
│   └── resources/
│       ├── application.yml
│       ├── schema.sql     # 建表（含 ai_error_log）
│       └── db/            # 迁移脚本（如 migration_add_ai_error_log.sql）
├── admin-ui/              # Vue3 管理前端
├── doc/                   # 文档（coze-useage、toutiao 分析等）
└── README.md
```

## 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 5.7+
- Node.js 16+

### 2. 数据库初始化

- 使用库名 **basesys**，执行 `src/main/resources/schema.sql` 建表（含 `ai_generation`、`ai_polish`、`ai_continuation`、**ai_error_log**）。
- 若库已存在且缺少表或列，可**一次性执行** `src/main/resources/db/init.sql`：创建 `ai_error_log` 表（若不存在），为 `ai_generation` 补充各平台文案列（`douyin_copy` / `xiaohongshu_copy` / `video_account_copy` / `toutiao_copy`），为 `ai_polish`、`ai_continuation` 补充 `platform` 列。可重复执行，已存在则跳过。

### 3. Coze 工作流配置

1. 在 Coze 工作台创建并发布工作流，支持 **generate / extend / beautify**（或按 operations 区分）。
2. 在 `application.yml` 中配置：
   - `coze.workflow.base-url`：发布地址（如 `https://xxx.coze.site`）
   - `coze.workflow.endpoint-path`：一般为 `/stream_run`
3. 设置环境变量 **COZE_WORKLOAD_IDENTITY_API_KEY**（Coze API Token）。

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

- 管理员：用户名 `admin`，密码见配置（如 `application.yml` 中 `app.default-password` 或项目说明）。

## 部署说明

- 后端：`mvn clean package` 后运行 `target/*.jar`。
- 前端：`npm run build`，将 `dist` 部署至 Web 服务器。
- 生产环境请配置 HTTPS、修改 JWT 密钥与默认密码、限制 CORS 与敏感接口。

## 许可证

MIT License