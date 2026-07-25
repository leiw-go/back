#!/bin/bash
# deploy-backend.sh — 启动/重启后端容器
# Usage: bash scripts/deploy-backend.sh <name> <port> <profile> <nacos_host> <nacos_port> <nacos_user> <nacos_pass> <network> <image> [db_url] [db_user] [db_pass] [jwt_secret] [jwt_expiration]
#   <network>: 选填,缺省 deploy-net;不存在则自动 docker network create
set -eu

BACKEND_NAME="${1:?usage: deploy-backend.sh <name> <port> <profile> <nacos_host> <nacos_port> <nacos_user> <nacos_pass> <network> <image>}"
BACKEND_PORT="${2:?backend port is required}"
PROFILE="${3:?profile is required}"
NACOS_HOST="${4:?nacos host is required}"
NACOS_PORT="${5:?nacos port is required}"
NACOS_USER="${6:?nacos username is required}"
NACOS_PASS="${7:?nacos password is required}"
NETWORK_NAME="${8:-deploy-net}"   # 缺省 deploy-net(老 Jenkins 任务清空该参数时不再报错)
BACKEND_IMAGE="${9:?backend image is required (e.g. leiw-go/back:latest)}"
DB_URL="${10:-}"
DB_USERNAME="${11:-}"
DB_PASSWORD="${12:-}"
JWT_SECRET="${13:-}"
JWT_EXPIRATION="${14:-86400000}"

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
NACOS_SERVER_ADDR=${NACOS_HOST}:${NACOS_PORT}
NACOS_USERNAME=${NACOS_USER}
NACOS_PASSWORD=${NACOS_PASS}
NACOS_NAMESPACE=
NACOS_GROUP=DEFAULT_GROUP
TZ=Asia/Shanghai
EOF

# prod 在 Nacos 不可用时依赖这些启动必需配置；空值不写入，保留 profile 自身默认值。
if [ -n "$DB_URL" ]; then printf 'DB_URL=%s\n' "$DB_URL" >> "$ENV_FILE"; fi
if [ -n "$DB_USERNAME" ]; then printf 'DB_USERNAME=%s\n' "$DB_USERNAME" >> "$ENV_FILE"; fi
if [ -n "$DB_PASSWORD" ]; then printf 'DB_PASSWORD=%s\n' "$DB_PASSWORD" >> "$ENV_FILE"; fi
if [ -n "$JWT_SECRET" ]; then printf 'JWT_SECRET=%s\n' "$JWT_SECRET" >> "$ENV_FILE"; fi
if [ -n "$JWT_EXPIRATION" ]; then printf 'JWT_EXPIRATION=%s\n' "$JWT_EXPIRATION" >> "$ENV_FILE"; fi
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
