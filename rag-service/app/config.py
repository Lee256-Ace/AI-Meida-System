from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    dashscope_api_key: str = ""
    qwen_model: str = "qwen-turbo"
    rag_port: int = 8001
    # 兼容 OpenAI 协议的 DashScope 兼容模式
    qwen_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    upload_dir: str = str(Path(__file__).resolve().parent.parent / "data" / "uploads")
    max_context_chars: int = 6000


@lru_cache
def get_settings() -> Settings:
    return Settings()
