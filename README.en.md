# AIMS · AI Multi-Platform Copywriting & Workflow System

An AI copywriting system built on **Spring Boot 3 + Vue 3**, supporting **copy generation**, **article polishing**, and **article continuation**. It calls the **Tongyi Qwen (Alibaba Cloud DashScope) LLM** for streaming generation, and persists error logs to a database for easy troubleshooting.

## Tech Stack

### Backend

- Spring Boot 3.2
- MyBatis 3.0
- MySQL 5.7+ (database `basesys`)
- Druid connection pool
- Jackson / Streaming SSE
- Alibaba Cloud DashScope SDK (Tongyi Qwen)

### Frontend

- Vue 3 + Element Plus
- Vue Router, Pinia
- Axios, ECharts (dashboard)

> **Note**: This repository is a **Java (Spring Boot) + Vue** monolithic application. Build and run use Maven / JDK 17+. The AI capability is provided by the **DashScope (Qwen) LLM API**, replacing the previous Coze cloud workflow.

## Features

### Copy Generation

- Enter a topic, select platforms (Douyin / Xiaohongshu / WeChat Channels / Toutiao), and generate multi-platform copy via the Qwen LLM.
- Supports **target audience** (general public / professionals / students / enterprise) and streaming output.
- Output format: one section per platform, with the platform name as a `【Platform Name】` heading.

### Article Polishing

- Paste the original text, choose a polishing type (simplify / enhance / grammar / rewrite) and target audience, and get streamed results.
- Polishing types: overall optimization / enhance style / simplify / adjust tone.

### Article Continuation

- Provide an opening and optional hints, choose **continuation length** (short / medium / long) and **style** (keep original / more formal / more vivid), and get streamed continuation content.

### Error Log & Troubleshooting

- **MySQL error log table** `basesys.ai_error_log`: records module, phase, error summary, stack trace, and request context for each module (generate / polish / continue / qwen / auth).
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
│   │   ├── controller/     # AiController, ErrorLogController, AuthController, etc.
│   │   ├── service/        # GenerationService, PolishService, ContinuationService, QwenWorkflowClient, ErrorLogService
│   │   ├── mapper/         # GenerationMapper, PolishMapper, ContinuationMapper, ErrorLogMapper
│   │   └── entity/         # GenerationTask, PolishTask, ContinuationTask, ErrorLog
│   └── resources/
│       ├── application.yml
│       ├── schema.sql      # DDL (including ai_error_log)
│       └── db/             # migration scripts (e.g. migration_add_ai_error_log.sql)
├── admin-ui/               # Vue 3 admin frontend
├── doc/                    # documentation
└── README.md
```

## Getting Started

### 1. Requirements

- JDK 17+
- MySQL 5.7+
- Node.js 16+

### 2. Database Initialization

- Use database **basesys** and run `src/main/resources/schema.sql` to create tables (`ai_generation`, `ai_polish`, `ai_continuation`, **ai_error_log**).
- If the database already exists and is missing tables or columns, run `src/main/resources/db/init.sql` once: it creates the `ai_error_log` table if absent, adds platform copy columns (`douyin_copy` / `xiaohongshu_copy` / `video_account_copy` / `toutiao_copy`) to `ai_generation`, and adds the `platform` column to `ai_polish` / `ai_continuation`. It is idempotent.

### 3. Qwen (DashScope) Configuration

1. Obtain a DashScope API Key from the Alibaba Cloud Model Studio console.
2. The API Key is configured directly in `QwenWorkflowClient.java` (constant `API_KEY`), as required.
3. The model name is configurable in `application.yml`:
   ```yaml
   qwen:
     model: qwen-plus
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

- Administrator: username `admin`, password configured in `application.yml` (see `app.default-password`).

## Deployment

- Backend: run `mvn clean package` then execute `target/*.jar`.
- Frontend: run `npm run build` and deploy the `dist` folder to a web server.
- In production, configure HTTPS, change the JWT secret and default password, and restrict CORS and sensitive endpoints.

## License

MIT License
