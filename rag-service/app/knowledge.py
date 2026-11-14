"""最小知识库：上传文件落盘 + 简单关键词检索（后续可换向量库）。"""

from __future__ import annotations

import re
import uuid
from dataclasses import dataclass, field
from pathlib import Path
from typing import List

from app.config import get_settings


@dataclass
class DocChunk:
    doc_id: str
    filename: str
    text: str


@dataclass
class KnowledgeStore:
    chunks: List[DocChunk] = field(default_factory=list)
    docs: dict = field(default_factory=dict)  # doc_id -> meta

    def add_text(self, filename: str, content: str) -> dict:
        doc_id = uuid.uuid4().hex[:12]
        text = (content or "").strip()
        if not text:
            raise ValueError("文件内容为空")

        # 按段落切块，块过大再按长度切
        pieces = _split_text(text, max_len=800)
        for p in pieces:
            self.chunks.append(DocChunk(doc_id=doc_id, filename=filename, text=p))
        meta = {
            "id": doc_id,
            "filename": filename,
            "chars": len(text),
            "chunks": len(pieces),
        }
        self.docs[doc_id] = meta
        return meta

    def list_docs(self) -> list[dict]:
        return list(self.docs.values())

    def retrieve(self, query: str, top_k: int = 4) -> list[DocChunk]:
        """关键词重叠打分检索；无文档时返回空。"""
        if not self.chunks or not query.strip():
            return []
        tokens = _tokenize(query)
        if not tokens:
            return self.chunks[:top_k]

        scored: list[tuple[float, DocChunk]] = []
        for chunk in self.chunks:
            score = _score(tokens, chunk.text)
            if score > 0:
                scored.append((score, chunk))
        scored.sort(key=lambda x: x[0], reverse=True)
        if not scored:
            # 兜底：返回最新若干块，保证最小链路可答
            return self.chunks[-top_k:]
        return [c for _, c in scored[:top_k]]


def _split_text(text: str, max_len: int = 800) -> list[str]:
    paras = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]
    if not paras:
        paras = [text]
    chunks: list[str] = []
    for p in paras:
        if len(p) <= max_len:
            chunks.append(p)
            continue
        for i in range(0, len(p), max_len):
            chunks.append(p[i : i + max_len])
    return chunks


def _tokenize(text: str) -> list[str]:
    # 中英文简单切词：连续中文单字 + 英文/数字词
    parts = re.findall(r"[\u4e00-\u9fff]|[A-Za-z0-9_]+", text.lower())
    # 过滤过短噪声
    return [p for p in parts if len(p) >= 1]


def _score(query_tokens: list[str], doc_text: str) -> float:
    doc_lower = doc_text.lower()
    hit = 0.0
    for t in query_tokens:
        if t in doc_lower:
            hit += 1.0 if len(t) > 1 else 0.3
    return hit


store = KnowledgeStore()


def ensure_upload_dir() -> Path:
    path = Path(get_settings().upload_dir)
    path.mkdir(parents=True, exist_ok=True)
    return path
