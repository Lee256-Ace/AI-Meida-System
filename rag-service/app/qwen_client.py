"""通义千问（DashScope OpenAI 兼容模式）调用封装 — 带限流与重试。"""

from __future__ import annotations

import asyncio
import json
import logging
from typing import AsyncIterator

import httpx

from app.config import get_settings

logger = logging.getLogger(__name__)

# ═══════════════════════════════════════════════════════
# 限流：根据 DashScope 套餐调整并发数
#   - 免费版 / 个人低配套餐：建议 1
#   - 标准版 / 企业套餐：可设为 2~5
# ═══════════════════════════════════════════════════════
_QWEN_SEM = asyncio.Semaphore(1)

# 重试配置
_MAX_RETRIES = 3
_BASE_DELAY = 1.0  # 秒

def _merge_system_for_stream(messages: list[dict]) -> list[dict]:
    """流式接口可能只允许 user/assistant，将 system 前置合并到 user。"""
    system_parts = [m["content"] for m in messages if m.get("role") == "system"]
    non_system = [m for m in messages if m.get("role") != "system"]

    if not system_parts:
        return messages

    merged_user_content = "\n\n".join(system_parts)
    if non_system and non_system[0].get("role") == "user":
        non_system[0] = {
            "role": "user",
            "content": merged_user_content + "\n\n" + non_system[0]["content"],
        }
    else:
        non_system.insert(0, {"role": "user", "content": merged_user_content})

    return non_system

async def chat_completion(
        messages: list[dict], stream: bool = False
) -> str | AsyncIterator[str]:
    settings = get_settings()
    if not settings.dashscope_api_key:
        raise RuntimeError(
            "未配置 DASHSCOPE_API_KEY。请在 rag-service/.env 中填写通义千问 API Key。"
        )

    if stream:
        messages = _merge_system_for_stream(messages)
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
        return await _request_with_retry(
            _sync_chat, url, headers, payload
        )

    # 流式：返回异步生成器（限流在生成器内部持有）
    return _stream_chat(url, headers, payload)


# ───────────────────────────────────────────────────────
# 同步（非流式）请求
# ───────────────────────────────────────────────────────

async def _sync_chat(url: str, headers: dict, payload: dict) -> str:
    async with httpx.AsyncClient(timeout=120.0) as client:
        resp = await client.post(url, headers=headers, json=payload)
        _raise_for_status(resp)
        data = resp.json()
        return data["choices"][0]["message"]["content"]


# ───────────────────────────────────────────────────────
# 流式请求（SSE）
# ───────────────────────────────────────────────────────

async def _stream_chat(url: str, headers: dict, payload: dict) -> AsyncIterator[str]:
    """流式生成器：在信号量保护下建立连接，逐 token 产出。"""
    async with _QWEN_SEM:
        async with httpx.AsyncClient(timeout=180.0) as client:
            async with client.stream("POST", url, headers=headers, json=payload) as resp:
                _raise_for_status(resp)
                async for token in _parse_sse(resp):
                    yield token


# ───────────────────────────────────────────────────────
# 重试包装器
# ───────────────────────────────────────────────────────

async def _request_with_retry(coro, *args, **kwargs):
    """指数退避重试：针对 429 / 5xx / 网络错误。"""
    last_exc = None
    for attempt in range(_MAX_RETRIES):
        async with _QWEN_SEM:
            try:
                return await coro(*args, **kwargs)
            except httpx.HTTPStatusError as e:
                status = e.response.status_code
                # 429 Too Many Requests 或 5xx 服务端错误 → 重试
                if status == 429 or status >= 500:
                    last_exc = e
                    delay = _BASE_DELAY * (2 ** attempt)
                    logger.warning(
                        "DashScope 请求失败 (%s)，%d/%d 次重试，等待 %.1fs…",
                        status, attempt + 1, _MAX_RETRIES, delay,
                                )
                    await asyncio.sleep(delay)
                    continue
                raise  # 4xx 客户端错误不重试
            except (httpx.NetworkError, httpx.TimeoutException) as e:
                last_exc = e
                delay = _BASE_DELAY * (2 ** attempt)
                logger.warning(
                    "DashScope 网络错误，%d/%d 次重试，等待 %.1fs…",
                    attempt + 1, _MAX_RETRIES, delay,
                    )
                await asyncio.sleep(delay)
                continue

    raise RuntimeError(
        f"DashScope 请求在 {_MAX_RETRIES} 次重试后仍失败: {last_exc}"
    ) from last_exc


# ───────────────────────────────────────────────────────
# SSE 解析（更健壮）
# ───────────────────────────────────────────────────────

async def _parse_sse(resp: httpx.Response) -> AsyncIterator[str]:
    """逐行解析 SSE 流，提取 content token。"""
    buffer = ""
    async for raw in resp.aiter_text():
        buffer += raw
        while "\n" in buffer:
            line, buffer = buffer.split("\n", 1)
            line = line.strip()
            if not line:
                continue

            if line.startswith("data:"):
                data = line[5:].strip()
            else:
                data = line.strip()

            if not data or data == "[DONE]":
                if data == "[DONE]":
                    return
                continue

            try:
                obj = json.loads(data)
            except json.JSONDecodeError:
                continue

            # 处理可能的 error 字段
            if "error" in obj:
                err = obj["error"]
                raise RuntimeError(f"DashScope 流式错误: {err}")

            choices = obj.get("choices", [])
            if not choices:
                continue

            delta = choices[0].get("delta", {})
            content = delta.get("content")
            if content:
                yield content


# ───────────────────────────────────────────────────────
# 状态码异常处理（提取服务端错误信息）
# ───────────────────────────────────────────────────────

def _raise_for_status(resp: httpx.Response) -> None:
    """在抛出异常前尝试提取服务端返回的详细错误信息。"""
    try:
        resp.raise_for_status()
    except httpx.HTTPStatusError as e:
        try:
            detail = resp.json()
        except Exception:
            detail = resp.text or "无详细错误信息"
        raise httpx.HTTPStatusError(
            f"{e.response.status_code} {e.response.reason_phrase}: {detail}",
            request=e.request,
            response=e.response,
        ) from None