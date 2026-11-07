#!/bin/bash
# deploy-prod.sh — 从本地构建镜像并发布到远程机器（docker compose 方式）
#
# Usage:
#   DEPLOY_HOST=root@1.2.3.4 ./scripts/deploy-prod.sh [tag]
#
# 环境变量：
#   DEPLOY_HOST       必填，ssh 目标 user@host
#   DEPLOY_DIR       远程部署目录，默认 /opt/greatbus
#   DEPLOY_PORT      ssh 端口，默认 22
#   DEPLOY_SSH_KEY   可选，ssh 私钥路径
#   BACKEND_IMAGE    镜像名，默认 leiw-go/back
#   BACKEND_PORT     容器对外端口，默认 8888
#   BACKEND_NETWORK  后端与 MySQL 共用的外部 docker 网络，默认 deploy-net
#   BACKEND_CONTAINER_NAME  容器名，默认 backend（nginx 反代依赖宿主 8888 端口）
#   DEPLOY_PLATFORM  目标架构，默认 linux/amd64（远程机一般是 x86_64）。
#                    本机是 Apple Silicon 时必须显式指定，否则镜像架构对不上。
#                    目标机是 arm64 就改成 linux/arm64。
#   SKIP_BUILD=1     跳过本地构建（复用本地已有镜像）
#   SKIP_ENV=1       不上传 env 文件（保留远程机现有配置）
#
# 流程：
#   本地 build → docker save | gzip → scp → 远端 docker load
#        → 上传 compose + env → 远端 docker compose up -d → 等健康检查
set -eu

# ---- 参数与环境 ----
TAG="${1:-${BACKEND_TAG:-latest}}"
DEPLOY_HOST="${DEPLOY_HOST:?必填环境变量 DEPLOY_HOST，例如 root@1.2.3.4}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/greatbus}"
DEPLOY_PORT="${DEPLOY_PORT:-22}"
BACKEND_IMAGE="${BACKEND_IMAGE:-leiw-go/back}"
BACKEND_PORT="${BACKEND_PORT:-8888}"
BACKEND_NETWORK="${BACKEND_NETWORK:-deploy-net}"
DEPLOY_PLATFORM="${DEPLOY_PLATFORM:-linux/amd64}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="${PROJECT_DIR}/deploy/backend.env"
COMPOSE_FILE="${PROJECT_DIR}/deploy/docker-compose.prod.yml"

IMAGE="${BACKEND_IMAGE}:${TAG}"

# ---- ssh / scp 参数 ----
SSH_OPTS=(-p "$DEPLOY_PORT" -o BatchMode=yes -o StrictHostKeyChecking=accept-new)
if [ -n "${DEPLOY_SSH_KEY:-}" ]; then
    SSH_OPTS+=(-i "$DEPLOY_SSH_KEY")
fi

# ---- 临时文件清理 ----
TMP_IMAGE="$(mktemp -t greatbus-image-XXXXXX.tar.gz)"
trap 'rm -f "$TMP_IMAGE"' EXIT

ssh_run() { ssh "${SSH_OPTS[@]}" "$DEPLOY_HOST" "$@"; }
scp_to() { scp -P "$DEPLOY_PORT" ${DEPLOY_SSH_KEY:+-i "$DEPLOY_SSH_KEY"} -o BatchMode=yes "$1" "$DEPLOY_HOST:$2"; }

# ---- 远端 sudo（部署目录不可写时需要）----
REMOTE_SUDO=""
if ssh_run "sudo -n true" >/dev/null 2>&1; then
    REMOTE_SUDO="sudo -n"
fi

# 上传文件到部署目录：目录可写就直接传，否则先传 /tmp 再 sudo mv
upload_to_deploy_dir() {
    local local_file="$1" remote_name="$2" mode="$3" tmp="/tmp/greatbus-$2"
    if [ -z "$REMOTE_SUDO" ] && ssh_run "test -w '$DEPLOY_DIR'" >/dev/null 2>&1; then
        scp_to "$local_file" "$DEPLOY_DIR/$remote_name"
        ssh_run "chmod $mode '$DEPLOY_DIR/$remote_name'"
    elif [ -n "$REMOTE_SUDO" ]; then
        scp_to "$local_file" "$tmp"
        ssh_run "$REMOTE_SUDO mkdir -p '$DEPLOY_DIR' && $REMOTE_SUDO mv '$tmp' '$DEPLOY_DIR/$remote_name' && $REMOTE_SUDO chmod $mode '$DEPLOY_DIR/$remote_name'"
    else
        echo "!! 部署目录 $DEPLOY_DIR 不可写，且远端没有免密 sudo"
        echo "   请换一个可写目录：DEPLOY_DIR=~/xxx ./scripts/deploy-prod.sh"
        exit 1
    fi
}

echo "==> 目标：$DEPLOY_HOST:$DEPLOY_PORT  目录：$DEPLOY_DIR"
echo "==> 镜像：$IMAGE  平台：$DEPLOY_PLATFORM  网络：$BACKEND_NETWORK"

# ---- 1. 本地构建 ----
if [ "${SKIP_BUILD:-0}" != "1" ]; then
    echo "==> [1/6] 本地构建镜像（platform=${DEPLOY_PLATFORM}）..."
    (cd "$PROJECT_DIR" && docker build \
        --platform "$DEPLOY_PLATFORM" \
        --build-arg DOCKER_MIRROR="$(bash "$PROJECT_DIR/scripts/pick-docker-mirror.sh")" \
        -f Dockerfile -t "$IMAGE" .)
else
    echo "==> [1/6] SKIP_BUILD=1，复用本地镜像 $IMAGE"
    docker image inspect "$IMAGE" >/dev/null 2>&1 || {
        echo "!! 本地不存在镜像 ${IMAGE}，请先构建或去掉 SKIP_BUILD"; exit 1; }
    ACTUAL_PLATFORM="$(docker image inspect "$IMAGE" --format '{{.Os}}/{{.Architecture}}')"
    echo "    本地镜像平台: $ACTUAL_PLATFORM"
    if [ "$ACTUAL_PLATFORM" != "$DEPLOY_PLATFORM" ]; then
        echo "!! 镜像平台($ACTUAL_PLATFORM)与目标($DEPLOY_PLATFORM)不一致，远端会起不来"
        echo "   请设 DEPLOY_PLATFORM=$ACTUAL_PLATFORM 或重新构建对应架构的镜像"
        exit 1
    fi
fi

# ---- 2. 导出并上传镜像 ----
echo "==> [2/6] 导出镜像并上传..."
docker save "$IMAGE" | gzip > "$TMP_IMAGE"
echo "    镜像大小: $(du -h "$TMP_IMAGE" | cut -f1)"
scp_to "$TMP_IMAGE" "/tmp/greatbus-image.tar.gz"

# ---- 3. 远端加载镜像 ----
echo "==> [3/6] 远端加载镜像..."
ssh_run "gunzip -c /tmp/greatbus-image.tar.gz | docker load && rm -f /tmp/greatbus-image.tar.gz"
REMOTE_PLATFORM="$(ssh_run "docker image inspect '$IMAGE' --format '{{.Os}}/{{.Architecture}}'")"
echo "    远端镜像平台: $REMOTE_PLATFORM"
if [ "$REMOTE_PLATFORM" != "$DEPLOY_PLATFORM" ]; then
    echo "!! 远端镜像架构($REMOTE_PLATFORM)与目标($DEPLOY_PLATFORM)不符，容器会起不来"
    exit 1
fi

# ---- 4. 上传编排文件与 env ----
echo "==> [4/6] 上传编排文件与 env..."
upload_to_deploy_dir "$COMPOSE_FILE" "docker-compose.prod.yml" "644"

if [ "${SKIP_ENV:-0}" != "1" ]; then
    if [ ! -f "$ENV_FILE" ]; then
        echo "!! 缺少 $ENV_FILE"
        echo "   先执行：cp deploy/backend.env.example deploy/backend.env 并填写真实值"
        echo "   （或用 SKIP_ENV=1 保留远端机现有配置）"
        exit 1
    fi
    # 防止把模板里的占位符传上去 —— 会让应用启动即崩（如 JWT_SECRET 非 base64）
    if grep -q 'CHANGE_ME' "$ENV_FILE"; then
        echo "!! $ENV_FILE 里还有 CHANGE_ME 占位符，拒绝上传"
        echo "   请先填写真实值，或用 SKIP_ENV=1 保留远端机现有配置"
        exit 1
    fi
    upload_to_deploy_dir "$ENV_FILE" "backend.env" "600"
else
    echo "    SKIP_ENV=1，保留远端机现有 backend.env"
fi

# ---- 5. 启动 ----
echo "==> [5/6] 远端 docker compose up -d..."
# 外部网络必须存在（MySQL 也在上面，后端要用容器名解析它）
if ! ssh_run "docker network inspect '$BACKEND_NETWORK' >/dev/null 2>&1"; then
    echo "!! 远端不存在 docker 网络 '$BACKEND_NETWORK'"
    echo "   后端需要与 MySQL 在同一网络才能通过容器名解析"
    echo "   先在远端执行：docker network create $BACKEND_NETWORK"
    exit 1
fi
# 清掉同名的旧容器（老的可能是 docker run 建的，compose 会因重名起不来）
CONTAINER_NAME="${BACKEND_CONTAINER_NAME:-backend}"
ssh_run "docker rm -f '$CONTAINER_NAME' >/dev/null 2>&1 || true"
ssh_run "cd '$DEPLOY_DIR' && BACKEND_IMAGE='$BACKEND_IMAGE' BACKEND_TAG='$TAG' BACKEND_PORT='$BACKEND_PORT' BACKEND_NETWORK='$BACKEND_NETWORK' SPRING_PROFILES_ACTIVE=prod \
         docker compose -f docker-compose.prod.yml up -d"

# ---- 6. 等健康检查 ----
echo "==> [6/6] 等待健康检查通过..."
for i in $(seq 1 40); do
    if ssh_run "curl -fsS http://localhost:$BACKEND_PORT/design/actuator/health" >/dev/null 2>&1; then
        echo "    ✅ 健康检查通过（约 $((i*5)) 秒）"
        ssh_run "cd '$DEPLOY_DIR' && docker compose -f docker-compose.prod.yml ps"
        echo
        echo "    接口地址：http://$DEPLOY_HOST:$BACKEND_PORT/design/swagger-ui.html"
        echo "    登录：POST /design/api/auth/login"
        exit 0
    fi
    sleep 5
done

echo "!! 200 秒内未就绪，远端容器日志："
ssh_run "docker logs --tail 100 '${CONTAINER_NAME:-backend}'" || true
exit 1
