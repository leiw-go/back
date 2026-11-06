#!/bin/bash
# deploy-backend.sh — 启动/重启后端容器
# Usage: bash scripts/deploy-backend.sh <name> <port> <profile> <network> <image> [db_url] [db_user] [db_pass] [jwt_secret] [jwt_expiration]
#   <network>: 选填,缺省 deploy-net;不存在则自动 docker network create
set -eu

BACKEND_NAME="${1:?usage: deploy-backend.sh <name> <port> <profile> <network> <image>}"
BACKEND_PORT="${2:?backend port is required}"
PROFILE="${3:?profile is required}"
NETWORK_NAME="${4:-deploy-net}"   # 缺省 deploy-net(老 Jenkins 任务清空该参数时不再报错)
BACKEND_IMAGE="${5:?backend image is required (e.g. leiw-go/back:latest)}"
DB_URL="${6:-}"
DB_USERNAME="${7:-}"
DB_PASSWORD="${8:-}"
JWT_SECRET="${9:-}"
JWT_EXPIRATION="${10:-86400000}"

# ---- network:不存在则自动创建 ----
if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    echo "==> network '$NETWORK_NAME' not found, creating..."
    docker network create "$NETWORK_NAME"
fi
NETWORK_ARGS=(--network "$NETWORK_NAME")
echo "==> joining network: $NETWORK_NAME"

# ---- env 文件（敏感凭据 → 容器内环境变量）----
ENV_FILE="$(mktemp)"
trap 'rm -f "$ENV_FILE"' EXIT
cat > "$ENV_FILE" <<EOF
SPRING_PROFILES_ACTIVE=${PROFILE}
TZ=Asia/Shanghai
EOF

# prod 的启动必需配置；空值不写入，保留 profile 自身默认值。
if [ -n "$DB_URL" ]; then printf 'DB_URL=%s\n' "$DB_URL" >> "$ENV_FILE"; fi
if [ -n "$DB_USERNAME" ]; then printf 'DB_USERNAME=%s\n' "$DB_USERNAME" >> "$ENV_FILE"; fi
if [ -n "$DB_PASSWORD" ]; then printf 'DB_PASSWORD=%s\n' "$DB_PASSWORD" >> "$ENV_FILE"; fi
if [ -n "$JWT_SECRET" ]; then printf 'JWT_SECRET=%s\n' "$JWT_SECRET" >> "$ENV_FILE"; fi
if [ -n "$JWT_EXPIRATION" ]; then printf 'JWT_EXPIRATION=%s\n' "$JWT_EXPIRATION" >> "$ENV_FILE"; fi

# 飞书 webhook 配置（可选）：从宿主机环境透传，未设置则用 application-{profile}.yml 的默认值。
# 注意 secret 走明文写入 env 文件，文件权限已在下面收紧为 600。
for FEISHU_KEY in FEISHU_WEBHOOK_URL FEISHU_WEBHOOK_ENABLED FEISHU_WEBHOOK_TIMEOUT_MS FEISHU_WEBHOOK_SECRET; do
    FEISHU_VALUE="$(printenv "$FEISHU_KEY" || true)"
    if [ -n "$FEISHU_VALUE" ]; then printf '%s=%s\n' "$FEISHU_KEY" "$FEISHU_VALUE" >> "$ENV_FILE"; fi
done
chmod 600 "$ENV_FILE"

# ---- 启动容器 ----
echo "==> starting ${BACKEND_NAME} (port ${BACKEND_PORT} → 8888)"
docker rm -f "$BACKEND_NAME" 2>/dev/null || true
docker run -d \
    --name "$BACKEND_NAME" \
    -p "${BACKEND_PORT}:8888" \
    --restart unless-stopped \
    "${NETWORK_ARGS[@]}" \
    --env-file "$ENV_FILE" \
    "$BACKEND_IMAGE"

# 确认进了网络
echo "==> network check:"
docker inspect -f '    {{.Name}}  networks: {{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' "$BACKEND_NAME"

# ---- 等后端就绪 ----
echo "==> waiting for backend..."
for i in $(seq 1 15); do
    sleep 3
    if docker exec "$BACKEND_NAME" sh -c 'curl -fsS http://localhost:8888/design/api-docs' >/dev/null 2>&1; then
        echo "==> backend ready after $((i*3))s"
        exit 0
    fi
    echo "    still waiting ($((i*3))s)..."
done

echo "!! backend not ready in 45s, use docker logs --tail 200 backend see why"
echo "   docker logs $BACKEND_NAME"
exit 1
