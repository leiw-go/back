#!/bin/bash
# publish-config.sh — 把本地 YAML 推送到 Nacos
# Usage: bash scripts/publish-config.sh <host> <port> <user> <pass> <profile> <config_repo_dir>
set -eu

NACOS_HOST="${1:?usage: publish-config.sh <host> <port> <user> <pass> <profile> <config_repo_dir>}"
NACOS_PORT="${2:?...}"
NACOS_USER="${3:?...}"
NACOS_PASS="${4:?...}"
PROFILE="${5:?...}"
CONFIG_REPO_DIR="${6:?...}"

FILE="${CONFIG_REPO_DIR}/ProjectInfomationManage-${PROFILE}.yml"
DATA_ID="ProjectInfomationManage-${PROFILE}.yaml"
GROUP="DEFAULT_GROUP"
BASE_URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos"

if [ ! -f "$FILE" ]; then
    echo "!! 配置文件不存在: $FILE"
    exit 1
fi

# Nacos 2.x OpenAPI 先用表单登录，再用 accessToken 访问配置接口。
LOGIN_BODY=$(mktemp)
trap 'rm -f "$LOGIN_BODY"' EXIT

if ! LOGIN_CODE=$(curl -sS -o "$LOGIN_BODY" -w '%{http_code}' \
    --max-time 5 \
    -X POST "${BASE_URL}/v1/auth/users/login" \
    --data-urlencode "username=${NACOS_USER}" \
    --data-urlencode "password=${NACOS_PASS}"); then
    echo "!! Nacos 登录接口不可达"
    exit 1
fi

if [ "$LOGIN_CODE" != "200" ]; then
    echo "!! Nacos 登录失败，HTTP $LOGIN_CODE"
    exit 1
fi

ACCESS_TOKEN=$(sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$LOGIN_BODY")
if [ -z "$ACCESS_TOKEN" ]; then
    echo "!! Nacos 登录响应中没有 accessToken"
    exit 1
fi

echo "==> publish ${DATA_ID} (group=${GROUP})"
if ! PUBLISH_RESULT=$(curl -fsS -X POST "${BASE_URL}/v1/cs/configs" \
    --data-urlencode "accessToken=${ACCESS_TOKEN}" \
    --data-urlencode "dataId=${DATA_ID}" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "content@${FILE}" \
    --data-urlencode "type=yaml"); then
    echo "!! Nacos 配置发布请求失败"
    exit 1
fi

if [ "$PUBLISH_RESULT" != "true" ]; then
    echo "!! Nacos 发布配置失败: $PUBLISH_RESULT"
    exit 1
fi
echo "    published OK"

# Nacos 持久化后会异步刷新配置缓存，短时间内读回可能返回 404。
echo "==> verifying..."
VERIFY_CODE=""
for ATTEMPT in 1 2 3 4 5; do
    if ! VERIFY_CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
        --max-time 5 \
        -G "${BASE_URL}/v1/cs/configs" \
        --data-urlencode "accessToken=${ACCESS_TOKEN}" \
        --data-urlencode "dataId=${DATA_ID}" \
        --data-urlencode "group=${GROUP}"); then
        echo "!! Nacos 配置读回请求失败"
        exit 1
    fi

    if [ "$VERIFY_CODE" = "200" ]; then
        echo "    verified OK"
        exit 0
    fi

    if [ "$VERIFY_CODE" != "404" ] || [ "$ATTEMPT" = "5" ]; then
        break
    fi
    sleep 1
done

echo "!! Nacos 配置读回失败，HTTP $VERIFY_CODE"
exit 1
