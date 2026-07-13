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

if [ ! -f "$FILE" ]; then
    echo "!! 配置文件不存在: $FILE"
    exit 1
fi

echo "==> publish ${DATA_ID} (group=${GROUP})"

# 发布
curl -fsS -X POST "http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/cs/configs" \
    -u "${NACOS_USER}:${NACOS_PASS}" \
    --data-urlencode "dataId=${DATA_ID}" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "content@${FILE}" \
    --data-urlencode "type=yaml" \
    && echo "    published OK"

# 读回验证
echo "==> verifying..."
curl -fsS "http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/cs/configs?dataId=${DATA_ID}&group=${GROUP}" \
    -u "${NACOS_USER}:${NACOS_PASS}" | head -10
