# AIMS RAG Service（私有知识库）

最小可跑链路：**上传文档 → 关键词检索 → 拼 Prompt → 通义千问 → 返回答案（SSE）**。

## 快速启动

```bash
cd rag-service
python -m venv .venv

# Windows
.venv\Scripts\activate

pip install -r requirements.txt
copy .env.example .env
# 编辑 .env，填入 DASHSCOPE_API_KEY

python run.py
```

服务默认：`http://localhost:8001`

- `GET  /health` 健康检查  
- `POST /upload` 上传 `.txt/.md/.json`  
- `POST /chat` 问答（`stream=true` 为 SSE）  
- `GET  /docs/list` 已入库文档  

## 前端联调

Vite 已代理 `/rag` → `http://localhost:8001`，页面路径：`/knowledge`。

## 后续可扩展

- 向量库（Chroma / FAISS）+ Embedding  
- PDF/DOCX 解析  
- 多轮对话记忆  
