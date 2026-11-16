"""最小知识库：上传文件落盘 + BM25 检索（零向量库依赖，效果远超关键词匹配）。"""

from __future__ import annotations

import re
import uuid
from dataclasses import dataclass, field
from pathlib import Path
from typing import List

import jieba
from rank_bm25 import BM25Okapi

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
    _bm25: BM25Okapi | None = field(default=None, repr=False)
    _tokenized_corpus: list[list[str]] = field(default_factory=list, repr=False)

    # ---------- 文档管理 ----------

    def add_text(self, filename: str, content: str) -> dict:
        doc_id = uuid.uuid4().hex[:12]
        text = (content or "").strip()
        if not text:
            raise ValueError("文件内容为空")

        pieces = _split_text(text, max_len=800, overlap=100)
        for p in pieces:
            self.chunks.append(DocChunk(doc_id=doc_id, filename=filename, text=p))

        meta = {
            "id": doc_id,
            "filename": filename,
            "chars": len(text),
            "chunks": len(pieces),
        }
        self.docs[doc_id] = meta
        self._rebuild_index()
        return meta

    def list_docs(self) -> list[dict]:
        return list(self.docs.values())

    # ---------- 检索 ----------

    def retrieve(self, query: str, top_k: int = 4) -> list[DocChunk]:
        """BM25 检索；无索引时兜底返回最新块。"""
        if not self.chunks or not query.strip():
            return []

        query_tokens = list(jieba.cut_for_search(query.strip()))
        if not query_tokens:
            return self.chunks[:top_k]

        if self._bm25 is None or not self._tokenized_corpus:
            return self.chunks[:top_k]

        scores = self._bm25.get_scores(query_tokens)
        top_indices = sorted(
            range(len(scores)), key=lambda i: scores[i], reverse=True
        )[:top_k]

        results = [self.chunks[i] for i in top_indices if scores[i] > 0]
        return results if results else self.chunks[:top_k]

    # ---------- 索引重建 ----------

    def _rebuild_index(self) -> None:
        self._tokenized_corpus = []
        for chunk in self.chunks:
            tokens = list(jieba.cut_for_search(chunk.text))
            self._tokenized_corpus.append(tokens)

        if self._tokenized_corpus:
            self._bm25 = BM25Okapi(self._tokenized_corpus)
        else:
            self._bm25 = None


# ---------- 分块工具 ----------


def _split_text(text: str, max_len: int = 800, overlap: int = 100) -> list[str]:
    """按段落 → 句子 → 固定长度三级切分，带重叠窗口保留上下文。"""
    paras = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]
    if not paras:
        paras = [text]

    chunks: list[str] = []
    for para in paras:
        if len(para) <= max_len:
            chunks.append(para)
            continue

        # 超长段落：按中文标点切句子
        raw = re.split(r"([。！？；\n])", para)
        sentences = ["".join(raw[i : i + 2]) for i in range(0, len(raw), 2)]

        current = ""
        for sent in sentences:
            sent = sent.strip()
            if not sent:
                continue

            if len(current) + len(sent) <= max_len:
                current += sent
            else:
                if current:
                    chunks.append(current.strip())
                current = sent

                # 单句就超长，硬切 + 重叠
                while len(current) > max_len:
                    chunks.append(current[:max_len])
                    current = current[max_len - overlap :]

        if current:
            chunks.append(current.strip())

    return chunks


store = KnowledgeStore()


def ensure_upload_dir() -> Path:
    path = Path(get_settings().upload_dir)
    path.mkdir(parents=True, exist_ok=True)
    return path