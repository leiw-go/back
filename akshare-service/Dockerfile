# akshare-service —— FastAPI 包装 akshare
#
# 基础镜像选用国内 daocloud 镜像源，拉取更稳。
#   docker.m.daocloud.io/library/python:3.11-slim  ← https://github.com/akfamily/akshare
#   文档站要求的 Python ≥ 3.8 即可；选择 3.11 是因为 akshare / pandas 在 3.11 上 wheel 完整。
#
# 镜像分层：
#   1. 装系统依赖（gcc / libffi-dev 用于编译部分 Python 包）
#   2. 装 Python 依赖（不变的层 → 缓存命中）
#   3. 拷应用代码（高频变动层 → 在最末）
#
# 预期体积 ~600MB（akshare + pandas + numpy 占大头）。

FROM docker.m.daocloud.io/library/python:3.11-slim

# 一些 akshare 依赖需要 C 编译；libffi-dev 用于 cryptography 等
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        gcc \
        libffi-dev \
 && rm -rf /var/lib/apt/lists/*

# 升级 pip / 用国内 PyPI 镜像加速
RUN python -m pip install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ \
        "pip>=24" "setuptools>=70" "wheel"

WORKDIR /app

# 先拷 requirements，最大化层复用
COPY requirements.txt .
RUN pip install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ -r requirements.txt

# 再拷应用代码
COPY app.py .

# FastAPI 默认 8000
EXPOSE 8000

# 容器启动超时设宽：akshare 冷启动（imports + 第一次网络）可能 10s+
# 单 worker 即可避免 akshare 的非线程安全缓存被并发踩到
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
