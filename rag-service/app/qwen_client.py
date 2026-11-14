"""通义千问（DashScope OpenAI 兼容模式）调用封装。"""

from __future__ import annotations

from typing import AsyncIterator

import httpx

from app.config import get_settings


async def chat_completion(messages: list[dict], stream: bool = False) -> str | AsyncIterator[str]:
    settings = get_settings()
    if not settings.dashscope_api_key:
        raise RuntimeError(
            "未配置 DASHSCOPE_API_KEY。请在 rag-service/.env 中填写通义千问 API Key。"
        )

    url = f"{settings.qwen_base_url.rstrip('/')}/chat/completions"
    headers = {
        "Authorization": f"Bearer {settings.dashscope_api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.qwen_model,
        "messages": messages,
        "stream": stream,
        "temperature": 0.3,
    }

    if not stream:
        async with httpx.AsyncClient(timeout=120.0) as client:
            resp = await client.post(url, headers=headers, json=payload)
            resp.raise_for_status()
            data = resp.json()
            return data["choices"][0]["message"]["content"]

    return _stream_chat(url, headers, payload)


async def _stream_chat(url: str, headers: dict, payload: dict) -> AsyncIterator[str]:
    async with httpx.AsyncClient(timeout=180.0) as client:
        async with client.stream("POST", url, headers=headers, json=payload) as resp:
            resp.raise_for_status()
            async for line in resp.aiter_lines():
                if not line:
                    continue
                if line.startswith("data:"):
                    data = line[5:].strip()
                else:
                    data = line.strip()
                if not data or data == "[DONE]":
                    if data == "[DONE]":
                        break
                    continue
                try:
                    import json

                    obj = json.loads(data)
                    delta = obj.get("choices", [{}])[0].get("delta", {})
                    content = delta.get("content")
                    if content:
                        yield content
                except Exception:
                    continue
