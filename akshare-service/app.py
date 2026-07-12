"""
FastAPI 包装层 —— 把 akshare (https://github.com/akfamily/akshare) 的部分接口暴露为 HTTP API。

设计原则：
- **白名单**调用：端点显式列出允许的 akshare 函数，不暴露通用 reflect 入口，避免安全风险。
- **超时与异常隔离**：akshare 内部会请求大量金融数据源（东财、新浪、腾讯…），任一站点抖动
  都可能 hang；统一在端点层 try/except，把异常转成 502 + 错误信息。
- **结构化返回**：akshare 多返回 pandas.DataFrame，统一 to_dict(orient='records') 后再 JSON 化。
- **健康检查**：/health 不依赖 akshare，容器启动后立即可服务（供 docker-compose / k8s 探活）。

启动方式：
    uvicorn app:app --host 0.0.0.0 --port 8000
"""

from __future__ import annotations

import json
import logging
from typing import Any

import akshare as ak
import pandas as pd
from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import JSONResponse

# ---------------------------------------------------------------------------- #
# 配置
# ---------------------------------------------------------------------------- #
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
log = logging.getLogger("akshare-service")

# 单次 akshare 调用上限：60s。akshare 内部请求经常卡住，宁愿 502 也不要拖垮整个 worker。
AKSHARE_CALL_TIMEOUT_SEC = 60

app = FastAPI(
    title="akshare-service",
    description="HTTP API wrapping https://github.com/akfamily/akshare
