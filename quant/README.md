# 聚宽量化策略 Python 模块

本目录包含与聚宽 (JoinQuant) SDK (`jqdatasdk`) 交互的 Python 实现.
Java 后端通过 `scripts/jq_cli.py` 启动一个子进程, 以 JSON 形式与 Python 通信, 避免
在 Spring Boot 项目中直接嵌入 Python 解释器.

## 目录结构

```
quant/
├── README.md                  # 本文件
├── requirements.txt           # Python 依赖
├── joinquant/
│   ├── __init__.py
│   └── client.py              # jqdatasdk 包装 + HTTP 远程 fallback
├── scripts/
│   ├── jq_cli.py              # CLI 入口 (Java 调用)
│   └── jq_server.py           # 可选 HTTP 远程服务
└── examples/
    └── sample_strategy.py     # 策略代码模板
```

## 安装依赖

```bash
cd quant
pip install -r requirements.txt
```

## 调用模式

Java 后端支持两种模式 (`application.yml` -> `quant.mode`):

* `PROCESS` (默认): 后端直接 `ProcessBuilder` 启动 `python3 scripts/jq_cli.py`,
  一次性调用, 适合低频操作 (如查询账户、下单、提交策略).
* `REMOTE`: 后端通过 HTTP 调用外部服务 (`quant.remote-base-url`),
  适合在生产环境把 jqdatasdk 部署在独立机器.

### PROCESS 模式

```bash
echo '{"action": "ping"}' | python3 quant/scripts/jq_cli.py
# {"ok": true, "data": {"ok": true, "mode": "LOCAL", "ts": ...}}
```

```bash
echo '{"action": "get_account_info"}' | python3 quant/scripts/jq_cli.py
# {"ok": true, "data": {"username": "...", "cash": 100000, ...}}
```

### REMOTE 模式

```bash
# 1) 启动 jqdatasdk 网关
export JQ_AUTH_USER=13800000000
export JQ_AUTH_PASS=your-password
python3 quant/scripts/jq_server.py    # 监听 :5000

# 2) 修改 application.yml -> quant.mode: REMOTE
# 3) Java 后端启动后即可通过 HTTP 调用 jqdatasdk
```

## 凭据安全

* 聚宽账户密码通过 `AesUtil` 在 Java 应用层使用 AES 加密后存储到 `t_quant_account.password`
* 数据库中保存的是密文, 不会泄露明文密码
* Python 进程在调用 `jqdatasdk.auth` 时, Java 后端会把明文密码通过 stdin 注入,
  **不**写入磁盘, **不**出现在日志中

## 扩展示例

要增加新接口 (如 `get_fundamentals`):

1. 在 `joinquant/client.py` 的 `JoinQuantClient` 和 `RemoteJoinQuantClient` 中
   实现对应方法
2. 在 `scripts/jq_cli.py` 的 `_dispatch` 函数中注册 action
3. (可选) 在 `scripts/jq_server.py` 中暴露 HTTP 路由
4. 在 Java 后端的 `JoinQuantInvoker` / Service 中封装调用
