#!/bin/bash
# check-nacos.sh — 验证 Nacos 可达
# Usage: bash scripts/check-nacos.sh <host> <port> [user] [pass]
set -eu

NACOS_HOST="${1:?usage: check-nacos.sh <host> <port> [user] [pass]}"
NACOS_PORT="${2:?usage: check-nacos.sh <host> <port> [user] [pass]}"
NACOS_USER="${3:-}"
NACOS_PASS="${4:-}"

URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos/"

echo "==> check Nacos: $URL"

# ---- 1. TCP 端口先通 ----
if ! timeout 5 bash -c "</dev/tcp/${NACOS_HOST}/${NACOS_PORT}" 2>/dev/null; then
    echo "!! TCP ${NACOS_HOST}:${NACOS_PORT} 不可达"
    echo "   排查清单："
    echo "     1) docker exec <jenkins容器> getent hosts ${NACOS_HOST}     # DNS 能不能解析"
    echo "     2) docker network inspect <NETWORK_NAME>                   # 容器是否在同一个网络里"
    echo "     3) docker ps --filter name=${NACOS_HOST}                    # Nacos 容器是否在跑"
    echo "     4) docker logs --tail 50 ${NACOS_HOST}                     # Nacos 启动日志"
    exit 1
fi
echo "    [1/2] TCP ${NACOS_PORT} OK"

# ---- 2. HTTP 200 ----
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$URL" || echo "000")
if [ "$HTTP_CODE" != "200" ]; then
    echo "!! HTTP ${URL} 返回 $HTTP_CODE（期望 200）"
    echo "   Nacos TCP 通但 HTTP 不通，看 Nacos 日志：docker logs ${NACOS_HOST}"
    exit 1
fi
echo "    [2/2] HTTP 200 OK"

# 凭据校验（可选）
if [ -n "$NACOS_USER" ] && [ -n "$NACOS_PASS" ]; then
    LOGIN_BODY=$(mktemp)
    trap 'rm -f "$LOGIN_BODY"' EXIT

    if ! LOGIN_CODE=$(curl -sS -o "$LOGIN_BODY" -w '%{http_code}' \
        --max-time 5 \
        -X POST "http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/auth/users/login" \
        --data-urlencode "username=${NACOS_USER}" \
        --data-urlencode "password=${NACOS_PASS}"); then
        echo "!! Nacos 登录接口不可达"
        exit 1
    fi

    echo "    [auth] login endpoint HTTP $LOGIN_CODE"
    case "$LOGIN_CODE" in
        200)
            ACCESS_TOKEN=$(sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$LOGIN_BODY")
            if [ -z "$ACCESS_TOKEN" ]; then
                echo "!! Nacos 登录成功但响应中没有 accessToken"
                exit 1
            fi
            ;;
        401|403)
            echo "!! Nacos 登录失败，请检查 NACOS_USER/NACOS_PASSWORD"
            exit 1
            ;;
        *)
            echo "!! Nacos 登录接口返回异常状态码: $LOGIN_CODE"
            exit 1
            ;;
    esac
fi

echo "==> Nacos OK"
