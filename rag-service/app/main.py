"""
AIMS RAG 私有知识库服务（最小可跑版本）

链路：上传文档 → 简易检索 → 拼 Prompt → 调通义千问 → 返回答案（支持 SSE 流式）
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.config import get_settings
from app.knowledge import ensure_upload_dir, store
from app.qwen_client import chat_completion

app = FastAPI(title="AIMS RAG Service", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户问题")
    stream: bool = Field(default=True, description="是否 SSE 流式输出")
    top_k: int = Field(default=4, ge=1, le=10)


class ChatResponse(BaseModel):
    answer: str
    contexts: list[dict]


def build_prompt(question: str, contexts: list) -> list[dict]:
    settings = get_settings()
    ctx_parts: list[str] = []
    total = 0
    for i, c in enumerate(contexts, 1):
        piece = f"[资料{i} | {c.filename}]\n{c.text}"
        if total + len(piece) > settings.max_context_chars:
            break
        ctx_parts.append(piece)
        total += len(piece)

    context_block = "\n\n".join(ctx_parts) if ctx_parts else "（当前知识库暂无相关资料）"
    system = (
        "你是 AIMS 私有知识库助手。请优先依据「参考资料」回答用户问题；"
        "资料不足时明确说明，并给出谨慎的一般性建议。回答使用简体中文，条理清晰。"
    )
    user = (
        f"【参考资料】\n{context_block}\n\n"
        f"【用户问题】\n{question.strip()}\n\n"
        "请基于参考资料作答。"
    )
    return [
        {"role": "system", "content": system},
        {"role": "user", "content": user},
    ]


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "docs": len(store.docs),
        "chunks": len(store.chunks),
        "model": get_settings().qwen_model,
        "api_key_configured": bool(get_settings().dashscope_api_key),
    }


@app.get("/docs/list")
async def list_docs():
    return {"list": store.list_docs()}


@app.post("/upload")
async def upload(file: UploadFile = File(...)):
    """上传知识库文件（.txt / .md / .json 纯文本）。"""
    filename = file.filename or "untitled.txt"
    suffix = Path(filename).suffix.lower()
    if suffix not in {".txt", ".md", ".markdown", ".json", ".csv", ".log"}:
        raise HTTPException(
            status_code=400,
            detail="当前最小版本仅支持文本类文件：.txt / .md / .json / .csv",
        )

    raw = await file.read()
    if not raw:
        raise HTTPException(status_code=400, detail="空文件")
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        try:
            text = raw.decode("gbk")
        except UnicodeDecodeError as e:
            raise HTTPException(status_code=400, detail="无法解码文件，请使用 UTF-8 或 GBK") from e

    upload_dir = ensure_upload_dir()
    save_path = upload_dir / f"{Path(filename).stem}_{Path(filename).suffix}"
    # 避免重名覆盖：加短 id
    from uuid import uuid4

    save_path = upload_dir / f"{Path(filename).stem}_{uuid4().hex[:8]}{suffix}"
    save_path.write_bytes(raw)

    try:
        meta = store.add_text(filename, text)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e

    return {"message": "上传成功", "doc": meta, "saved_as": save_path.name}


@app.post("/chat")
async def chat(body: ChatRequest):
    """问答：检索 → 拼 Prompt → 调 Qwen。stream=true 时返回 SSE。"""
    question = body.question.strip()
    if not question:
        raise HTTPException(status_code=400, detail="问题不能为空")

    contexts = store.retrieve(question, top_k=body.top_k)
    messages = build_prompt(question, contexts)
    ctx_payload = [
        {"doc_id": c.doc_id, "filename": c.filename, "snippet": c.text[:200]}
        for c in contexts
    ]

    if not body.stream:
        try:
            answer = await chat_completion(messages, stream=False)
        except Exception as e:
            raise HTTPException(status_code=502, detail=f"调用 Qwen 失败: {e}") from e
        return ChatResponse(answer=str(answer), contexts=ctx_payload)

    async def event_gen():
        # 先推送检索到的上下文，便于前端展示
        yield f"event: contexts\ndata: {json.dumps(ctx_payload, ensure_ascii=False)}\n\n"
        try:
            stream = await chat_completion(messages, stream=True)
            async for token in stream:  # type: ignore[union-attr]
                yield f"event: token\ndata: {json.dumps(token, ensure_ascii=False)}\n\n"
            yield "event: done\ndata: [DONE]\n\n"
        except Exception as e:
            yield f"event: error\ndata: {json.dumps(str(e), ensure_ascii=False)}\n\n"

    return StreamingResponse(event_gen(), media_type="text/event-stream")


def create_app() -> FastAPI:
    return app
