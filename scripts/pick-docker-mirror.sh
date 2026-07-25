#!/bin/sh
# ============================================================
# pick-docker-mirror.sh - 探测多个 docker 镜像源，输出第一个可用的
#
# 由 Jenkinsfile stage 1 在 docker build 前调用，规避单一镜像源挂掉导致 build 全失败。
#
# 用法（手动跑也行）：
#   bash ./scripts/jenkins/pick-docker-mirror.sh
#
# 环境变量（可选）：
#   MIRRORS="dockerproxy.cn 1ms.run ..."   自定义候选列表（空格分隔）
#   PREFERRED="docker.m.1ms.run"           优先尝试这个（仍会探测其他）
#   TIMEOUT=5                               每个源探测超时秒数
#
# 输出：
#   - 选中的镜像源写到 stdout（无尾换行），供 Jenkins returnStdout 直接捕获
#   - 探测日志写到 stderr，在 Jenkins console 可见
#   - 退出码：0 = 找到；1 = 全部不可用
#
# 检测逻辑：
#   curl https://${host}/v2/ 5 秒超时
#   状态码 2xx 或 401 都算活着 —— 401 是 docker registry 公开 /v2/ 的标准行为
#   （要求走 /v2/library/<image>/manifests/<tag> 拉具体 tag）
# ============================================================
set -eu

# ---- 候选列表（按历史可用性排序） ----
: "${MIRRORS:=dockerproxy.cn docker.m.1ms.run hub-mirror.c.163.com docker.m.daocloud.io registry-1.docker.io}"
: "${PREFERRED:=}"
: "${TIMEOUT:=5}"

log() { printf '[pick-mirror] %s\n' "$*" >&2; }

# ---- 构造有序候选：PREFERRED 置首，其他去重 ----
ORDERED=""
if [ -n "$PREFERRED" ]; then
  ORDERED="$PREFERRED"
fi
for m in $MIRRORS; do
  case " $ORDERED " in
    *" $m "*) ;;
    *) ORDERED="$ORDERED $m" ;;
  esac
done

log "候选（按序）：$ORDERED"
log "每源超时：${TIMEOUT}s"

# ---- 单源探测：返回 0 表示活着 ----
probe() {
  host="$1"
  code=$(curl --connect-timeout "$TIMEOUT" --max-time "$TIMEOUT" \
    -s -o /dev/null -w '%{http_code}' \
    "https://${host}/v2/" 2>/dev/null || echo "000")
  case "$code" in
    2*|401) return 0 ;;
    *) return 1 ;;
  esac
}

# ---- 主循环：返回第一个活着 ----
for m in $ORDERED; do
  if probe "$m"; then
    log "✓ $m"
    printf '%s' "$m"
    exit 0
  else
    log "✗ $m"
  fi
done

log "ERROR: 全部镜像源不可达 —— 请检查本机 DNS 或出网代理"
exit 1
