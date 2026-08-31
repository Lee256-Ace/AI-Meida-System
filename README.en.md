# AIMS · AI Multi-Platform Copywriting & Workflow System

An AI self-media copywriting system built on **Spring Boot 3 + Vue 3**, supporting **copy generation**, **article polishing**, **article continuation**, **news hotspot management**, **Markdown editor**, **publish history**, and **user management**. AI capabilities are provided by the **Qwen (Alibaba Cloud DashScope) LLM** — `qwen-plus` for generation and `qwen-turbo` for news summaries — with built-in error-log persistence and scheduled cleanup tasks for easy troubleshooting and maintenance.

## Tech Stack

### Backend

- Spring Boot 3.2
- MyBatis + MyBatis-Plus
- MySQL 5.7+ (database `basesys`)
- Druid connection pool
- Jackson / Streaming SSE
- Alibaba Cloud DashScope SDK (Qwen: `qwen-plus` for generation, `qwen-turbo` for summaries)
- HtmlUnit + Jsoup (Sina news hotspot crawler)
- Hutool, Lombok, Spring `@Scheduled` scheduled tasks

### Frontend

- Vue 3 + Element Plus
- Vue Router, Pinia
- Axios, ECharts (dashboard)

> **Note**: This repository is a **Java (Spring Boot) + Vue** monolithic application. Build and run use Maven / JDK 17+. The AI capability is provided by the **DashScope (Qwen) LLM API**, replacing the previous Coze cloud workflow. There is no Python entry point or `.coze` runtime configuration.

## Features

### Copy Generation

- Enter a topic, select platforms (Douyin / Xiaohongshu / WeChat Channels / Toutiao), and generate multi-platform copy via the Qwen LLM (`qwen-plus`).
- Supports **target audience** (general public / professionals / students / enterprise) and streaming output.
- Output format: one section per platform, with the platform name as a `【Platform Name】` heading.

### Article Polishing

- Paste the original text, choose a polishing type (overall optimization / enhance style / simplify / adjust tone) and target audience, and get streamed results.

### Article Continuation

- Provide an opening and optional hints, choose **continuation length** (short / medium / long) and **style** (keep original / more formal / more vivid), and get streamed continuation content.

### News Hotspot Management

- Crawls Sina news hotspots (domestic / international / society / sports / finance / entertainment / technology) using **HtmlUnit + Jsoup**, with paginated queries and filtering by title / type.
- **AI summary**: uses Qwen `qwen-turbo` to generate a summary of the news content. The result is cached in the `article_news.summary` column to avoid repeated LLM calls (token cost reduction).
- **Scheduled cleanup**: the `NewsCleanupTask` periodically removes outdated hotspots. The retention period and cron expression are configured via `news.cleanup` in `application.yml` (default: keep 3 days, run at 3:00 AM daily).

### Markdown Editor

- Edit Markdown copy online, with save/update support (insert on first save, update on subsequent saves in the `ai_markdown_copy` table).

### Publish History & Platform Management

- Maintains publish platforms (`article_platform`) and records publish history (`publish_history`).

### Users & Permissions

- Dual tables for administrators (`admins`) and regular users (`users`), JWT authentication, and user management (CRUD, enable/disable).

### Dashboard

- An ECharts-based statistics dashboard visualizing copy generation and hotspot data.

### Error Log & Troubleshooting

- **MySQL error log table** `basesys.ai_error_log`: records module, phase, error summary, stack trace, and request context for each module (generate / polish / continue / qwen / auth, etc.).
- **Error messages** returned to the frontend include the module, phase, and reason, e.g. `[Module] phase "xxx" failed: reason. Error has been logged to system.`
- **Error log API**:
  - `GET /api/error-log/list?module=&page=1&size=20` — paginated query, optional filter by `module`.
  - `GET /api/error-log/{id}` — single record details (including full `error_detail` stack trace).
  - `DELETE /api/error-log/{id}` — delete a single record.

## Project Structure

```
aims/
├── src/main/
│   ├── java/com/product/
│   │   ├── controller/     # AiController, ArticleNewsController, ArticlePlatformController, AuthController,
│   │   │                   # DashboardController, ErrorLogController, HistoryController, MarkdownCopyController,
│   │   │                   # PublishHistoryController, UploadController, UserManagementController
│   │   ├── service/        # GenerationService, PolishService, ContinuationService, ArticleNewsService,
│   │   │                   # QwenWorkflowClient, ErrorLogService, AuthService, UserManagementService, etc.
│   │   ├── mapper/         # MyBatis mapper interfaces
│   │   ├── entity/         # GenerationTask, PolishTask, ContinuationTask, ArticleNews, ArticlePlatform,
│   │   │                   # Admin, AppUser, MarkdownCopy, PublishHistory, ErrorLog, etc.
│   │   ├── spider/         # SinaHotNewsSpider (Sina news hotspot crawler)
│   │   ├── task/           # NewsCleanupTask (scheduled hotspot cleanup)
│   │   ├── util/           # JwtUtil, etc.
│   │   └── config/ dto/ vo/ param/ common/ constant/
│   └── resources/
│       ├── application.yml  # includes spring.sql.init auto-initialization config
│       ├── schema.sql       # authoritative DDL (11 tables, idempotent via IF NOT EXISTS)
│       ├── mapper/          # MyBatis XML
│       └── db/              # incremental migration scripts (e.g. init.sql)
├── admin-ui/               # Vue 3 admin frontend (Vite)
├── doc/                    # documentation (thesis specifications, format guides, etc.)
└── README.md
```

## Getting Started

### 1. Requirements

- JDK 17+
- MySQL 5.7+
- Node.js 16+

### 2. Database Initialization

- Use database **basesys**. `src/main/resources/schema.sql` is the **authoritative DDL script** containing all **11 tables**, all using `CREATE TABLE IF NOT EXISTS` (idempotent, safe to re-run):

  | Table | Description |
  | --- | --- |
  | `admins` | Administrator table (with default account `admin/admin123`) |
  | `users` | Regular user table |
  | `ai_generation` | Copy generation |
  | `ai_polish` | Article polishing |
  | `ai_continuation` | Article continuation |
  | `ai_task` | AI task records |
  | `ai_error_log` | Error log |
  | `ai_markdown_copy` | Markdown copy drafts |
  | `article_news` | News hotspots (with `summary` caching column) |
  | `article_platform` | Publish platforms |
  | `publish_history` | Publish history |

- **Auto-initialization**: `application.yml` has `spring.sql.init.mode: always` enabled, so the backend automatically runs `classpath:schema.sql` on startup (`IF NOT EXISTS` keeps it idempotent and safe on repeated starts).
- If the database already exists and is missing tables or columns, run `src/main/resources/db/init.sql` once: it uses stored procedures to check column existence, adds platform copy columns (`douyin_copy` / `xiaohongshu_copy` / `video_account_copy` / `toutiao_copy`) to `ai_generation`, adds the `platform` column to `ai_polish` / `ai_continuation`, and migrates legacy `admins` data to `users`. It is idempotent.

### 3. Qwen (DashScope) Configuration

1. Obtain a DashScope API Key from the Alibaba Cloud Model Studio console.
2. The API Key is configured directly in `QwenWorkflowClient.java` (constant `API_KEY`), as required.
3. The model names are configurable in `application.yml`:

   ```yaml
   qwen:
     model: qwen-plus          # model used for generation/polish/continuation
     summary-model: qwen-turbo # lightweight model used for news summaries (token cost reduction)
   ```

### 4. Run the Backend

```bash
cd aims
mvn spring-boot:run
```

### 5. Run the Frontend

```bash
cd admin-ui
npm install
npm run dev
```

### 6. Access

- Frontend: <http://localhost:3000>
- Backend API: <http://localhost:8080/api>
- Druid monitor: <http://localhost:8080/api/druid>
- Error log list (example): <http://localhost:8080/api/error-log/list>

## Test Account

- Administrator: `admin` / `admin123` (auto-created by the `schema.sql` seed data).
- Regular user: create via the User Management page, or use the default password when registering (`app.default-password` in `application.yml`, default `root`).

## Deployment

- Backend: run `mvn clean package` then execute `target/*.jar`.
- Frontend: run `npm run build` and deploy the `dist` folder to a web server.
- In production, configure HTTPS, change the JWT secret and default password, and restrict CORS and sensitive endpoints.

## License

MIT License
