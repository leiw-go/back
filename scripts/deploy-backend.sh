#!/bin/bash
# deploy-backend.sh — 启动/重启后端容器
# Usage: bash scripts/deploy-backend.sh <name> <port> <profile> <nacos_host> <nacos_port> <nacos_user> <nacos_pass> <network> <image>
set -eu

BACKEND_NAME="${1:?usage: deploy-backend.sh <name> <port> <profile> <nacos_host> <nacos_port> <nacos_user> <nacos_pass> <network> <image>}"
BACKEND_PORT="${2:?...}"
PROFILE="${3:?...}"
NACOS_HOST="${4:?...}"
NACOS_PORT="${5:?...}"
NACOS_USER="${6:?...}"
NACOS_PASS="${7:?...}"
NETWORK_NAME="${8:-}"
BACKEND_IMAGE="${9:?...}"

# ---- network 参数 ----
NETWORK_ARGS=()
if [ -n "$NETWORK_NAME" ]; then
    NETWORK_ARGS=(--network "$NETWORK_NAME")
    echo "==> joining network: $NETWORK_NAME"
else
    echo "!! NETWORK_NAME 为空：后端容器会跑在默认 bridge，连不上同网络的 nacos/mysql"
fi

# ---- env 文件（敏感凭据 → 容器内环境变量）----
ENV_FILE="$(mktemp)"
trap 'rm -f "$ENV_FILE"' EXIT
cat > "$ENV_FILE" <<EOF
SPRING_PROFILES_ACTIVE=${PROFILE}
SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=${NACOS_HOST}:${NACOS_PORT}
SPRING_CLOUD_NACOS_CONFIG_USERNAME=${NACOS_USER}
SPRING_CLOUD_NACOS_CONFIG_PASSWORD=${NACOS_PASS}
SPRING_CLOUD_NACOS_CONFIG_NAMESPACE=
SPRING_CLOUD_NACOS_CONFIG_GROUP=DEFAULT_GROUP
SPRING_CLOUD_NACOS_CONFIG_FILE_EXTENSION=yaml
TZ=Asia/Shanghai
EOF
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
if [ -n "$NETWORK_NAME" ]; then
    echo "==> network check:"
    docker inspect -f '    {{.Name}}  networks: {{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' "$BACKEND_NAME"
fi

# ---- 等后端就绪 ----
echo "==> waiting for backend..."
for i in $(seq 1 15); do
    sleep 3
    if docker exec "$BACKEND_NAME" sh -c 'curl -fsS http://localhost:8888/api-docs' >/dev/null 2>&1; then
        echo "==> backend ready after $((i*3))s"
        exit 0
    fi
    echo "    still waiting ($((i*3))s)..."
done

echo "!! backend not ready in 45s"
echo "   docker logs $BACKEND_NAME"
exit 1
