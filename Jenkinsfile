// ============================================================
// Jenkinsfile — 部署 Backend 到本机 Docker
//   - 假设 backend / mysql / jenkins / nacos 已在同一 docker user-defined 网络里
//   - 容器之间用「容器名」互通（Docker 内置 DNS）
//   - 负责：构建镜像 -> 推送配置到 Nacos -> 启动后端容器（加入同一网络）
//   - 敏感配置（DB 密码、JWT secret）由 Nacos 提供，不写在 Jenkinsfile
// ============================================================
pipeline {
    agent any

    parameters {
        string(name: 'BACKEND_TAG',   defaultValue: 'latest',  description: '镜像 tag')
        booleanParam(name: 'CLEAN_IMAGE', defaultValue: false, description: '构建前是否清理同名旧镜像')
        string(name: 'PROFILE',       defaultValue: 'dev',     description: 'Spring profile (dev|prod)')

        // 后端容器相关
        string(name: 'BACKEND_PORT',  defaultValue: '8888',    description: '后端宿主机暴露端口')
        string(name: 'BACKEND_NAME',  defaultValue: 'backend', description: '后端容器名')

        // 后端容器看到的 Nacos 地址
        //   所有服务在同一 docker 网络里，用「容器名」即可（Docker 内置 DNS 自动解析）。
        string(name: 'NACOS_HOST',    defaultValue: 'nacos',
               description: 'Nacos 主机（后端容器视角）。同网络内直接用容器名 nacos。')
        string(name: 'NACOS_PORT',    defaultValue: '8848',    description: 'Nacos HTTP 端口')
        string(name: 'NACOS_USER',    defaultValue: 'nacosadmin',   description: 'Nacos 用户名')
        string(name: 'NACOS_PASSWORD', defaultValue: 'zVndnMGgkytNH7V0iJg1eqc1hwcTSq9',  description: 'Nacos 密码')

        // Jenkins agent 自己用来 ping Nacos 的地址
        //   Jenkins 也在同一网络里，所以也直接用容器名。如果 Jenkins 跑在网络外，
        //   可以改成宿主机网关 IP 或 host.docker.internal（视启动参数而定）。
        string(name: 'NACOS_CHECK_HOST', defaultValue: 'nacos',
               description: 'Jenkins agent ping Nacos 用的地址。默认 nacos（同网络）。')

        // 后端容器要加入的 docker 网络（必须和 nacos/mysql 同一个）
        //   docker network ls 看一下填进去；留空表示不指定 --network（容器跑在默认 bridge，可能连不上 nacos）。
        string(name: 'NETWORK_NAME',  defaultValue: '',
               description: 'docker network 名（所有服务所在的网络）。docker network ls 查看。')

        // 配置发布：本地 YAML 目录（不入 git，作为 Nacos 的发布源）
        string(name: 'CONFIG_REPO_DIR', defaultValue: '',
               description: '本地 YAML 配置目录（留空 = 跳过配置发布）')
    }

    environment {
        BACKEND_IMAGE = 'leiw-go/back'
    }

    options {
        timeout(time: 20, unit: 'MINUTES')
        // 不允许并发执行：避免 8888 端口冲突
        disableConcurrentBuilds()
    }

    stages {

        stage('1. Check Nacos') {
            steps {
                // 先用 Groovy 把参数绑定成 shell 变量，避免后续在 GString 里写多个 ${params.X}
                script {
                    env.NACOS_CHECK_HOST = params.NACOS_CHECK_HOST
                    env.NACOS_CHECK_PORT = params.NACOS_PORT
                    env.NACOS_CHECK_USER = params.NACOS_USER
                    env.NACOS_CHECK_PASS = params.NACOS_PASSWORD
                }
                sh '''
                    set +e
                    echo '======================================================'
                    echo '   Nacos connectivity diagnostics'
                    echo "   target: ${NACOS_CHECK_HOST}:${NACOS_CHECK_PORT}"
                    echo '======================================================'

                    # --- A. 当前 build agent 的网络身份 ---
                    echo
                    echo '---- [A] 自身网络信息 ----'
                    echo "$ hostname       : $(hostname)"
                    echo '$ cat /etc/hosts :'
                    cat /etc/hosts | sed 's/^/    /'
                    echo '$ ip addr (摘录) :'
                    ip -4 addr show 2>/dev/null | grep -E 'inet |^[0-9]+:' | sed 's/^/    /' || echo '    (ip 命令不可用)'
                    echo '$ ip route       :'
                    ip route 2>/dev/null | sed 's/^/    /' || echo '    (ip route 不可用)'

                    # --- B. DNS 解析 ---
                    echo
                    echo '---- [B] DNS 解析 ----'
                    for h in host.docker.internal gateway.docker.internal localhost; do
                        ip_of_h=$(getent hosts "$h" 2>/dev/null | awk '{print $1}' | head -1)
                        if [ -n "$ip_of_h" ]; then
                            echo "    $h -> $ip_of_h"
                        else
                            echo "    $h -> (无法解析)"
                        fi
                    done

                    # --- C. Nacos 容器（在 Jenkins 容器里能不能直接看到 docker）---
                    echo
                    echo '---- [C] 宿主机 / 容器侧的 nacos 状态 ----'
                    if command -v docker >/dev/null 2>&1; then
                        echo '    docker 可用，列出 nacos 相关容器：'
                        docker ps -a --filter name=nacos --format '    {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>&1 | sed 's/^/    /'
                        echo '    端口映射:'
                        docker port nacos 2>&1 | sed 's/^/    /'
                        echo '    最近 20 行日志:'
                        docker logs --tail 20 nacos 2>&1 | sed 's/^/    /'
                    else
                        echo '    docker 命令不可用 — Jenkins agent 不在能管 docker 的容器里'
                        echo "    这种情况下 NACOS_CHECK_HOST 必须用宿主机 IP（不是 host.docker.internal）"
                    fi

                    # --- D. 实际打 Nacos（verbose，输出每个失败原因）---
                    echo
                    echo '---- [D] 直接 curl Nacos ----'
                    echo "    curl -v --max-time 5 http://${NACOS_CHECK_HOST}:${NACOS_CHECK_PORT}/nacos/"
                    curl -v --max-time 5 "http://${NACOS_CHECK_HOST}:${NACOS_CHECK_PORT}/nacos/" 2>&1 | sed 's/^/    /'
                    # 注意：管道后 $? 是 sed 的退出码，必须用 PIPESTATUS[0] 拿 curl 的
                    CURL_EXIT=${PIPESTATUS[0]}

                    # --- E. 候选地址扫描 ---
                    echo
                    echo '---- [E] 候选地址扫描 ----'
                    # 同 docker 网络时，「容器名 nacos」是首选候选；其余作为兜底
                    CANDIDATES="nacos host.docker.internal gateway.docker.internal localhost 127.0.0.1"
                    GW=$(ip route 2>/dev/null | awk '/default/ {print $3; exit}')
                    [ -n "$GW" ] && CANDIDATES="$CANDIDATES $GW"
                    for h in $CANDIDATES; do
                        RESULT=$(curl -s -o /dev/null -w '%{http_code} (%{time_total}s)' \
                                       --max-time 3 "http://$h:${NACOS_CHECK_PORT}/nacos/" 2>&1)
                        echo "    http://$h:${NACOS_CHECK_PORT}/nacos/  ->  $RESULT"
                    done

                    # --- F. 端口层探测 ---
                    echo
                    echo '---- [F] TCP 端口探测 ----'
                    for h in nacos host.docker.internal gateway.docker.internal localhost 127.0.0.1 $GW; do
                        [ -z "$h" ] && continue
                        if timeout 3 bash -c "</dev/tcp/$h/${NACOS_CHECK_PORT}" 2>/dev/null; then
                            echo "    $h:${NACOS_CHECK_PORT}    TCP OK"
                        else
                            echo "    $h:${NACOS_CHECK_PORT}    TCP closed/timeout"
                        fi
                    done

                    echo
                    echo '======================================================'
                    echo "   curl 主目标退出码: $CURL_EXIT"
                    echo '======================================================'

                    # 主目标必须通，否则 fail 这个 stage
                    if [ "$CURL_EXIT" != "0" ]; then
                        echo
                        echo "!! 主目标 http://${NACOS_CHECK_HOST}:${NACOS_CHECK_PORT}/nacos/ 不可达"
                        echo "   请根据上方 [A]~[F] 段的输出定位问题:"
                        echo "     - [B] nacos 解析不到            -> Jenkins 容器不在 nacos 所在的网络，检查 docker network inspect <NETWORK_NAME>"
                        echo "     - [C] 看不到 docker 容器         -> Jenkins agent 没挂 docker.sock"
                        echo "     - [C] nacos 容器没在跑           -> 先 docker start nacos"
                        echo "     - [F] 全部 closed/timeout        -> 端口没映射 / 跨网络不通，确认 NETWORK_NAME 一致"
                        exit 1
                    fi
                '''
            }
        }

        stage('2. Publish Config to Nacos') {
            when { expression { return params.CONFIG_REPO_DIR?.trim() } }
            steps {
                sh """
                    set -eux
                    FILE='${params.CONFIG_REPO_DIR}/ProjectInfomationManage-${params.PROFILE}.yml'
                    test -f "\$FILE" || { echo "!! missing config file: \$FILE"; exit 1; }

                    DATA_ID='ProjectInfomationManage-${params.PROFILE}.yaml'
                    GROUP='DEFAULT_GROUP'

                    # 发布配置
                    curl -fsS -X POST "http://${params.NACOS_HOST}:${params.NACOS_PORT}/nacos/v1/cs/configs" \\
                        -u '${params.NACOS_USER}:${params.NACOS_PASSWORD}' \\
                        --data-urlencode "dataId=\${DATA_ID}" \\
                        --data-urlencode "group=\${GROUP}" \\
                        --data-urlencode "content@\${FILE}" \\
                        --data-urlencode "type=yaml" \\
                        && echo "==> published \${DATA_ID}"

                    # 读回验证
                    echo '==> verifying...'
                    curl -fsS "http://${params.NACOS_HOST}:${params.NACOS_PORT}/nacos/v1/cs/configs?dataId=\${DATA_ID}&group=\${GROUP}" \\
                        -u '${params.NACOS_USER}:${params.NACOS_PASSWORD}' | head -10
                """
            }
        }

        stage('3. Build Backend Image') {
            steps {
                script {
                    if (params.CLEAN_IMAGE) {
                        sh "docker rmi -f ${BACKEND_IMAGE}:${params.BACKEND_TAG} || true"
                    }
                }
                sh "docker build -f Dockerfile -t ${BACKEND_IMAGE}:${params.BACKEND_TAG} ."
                sh "docker images | grep ${BACKEND_IMAGE} || true"
            }
        }

        stage('4. Deploy Backend Container') {
            steps {
                // env 文件只注入 Nacos 地址与凭据；敏感配置（datasource / jwt）由 Nacos 提供
                // --network：把后端容器加进所有服务所在的那个 user-defined 网络，
                //   这样容器名 nacos / mysql 才能被 Docker 内置 DNS 解析。
                sh """
                    set -eux

                    # 构造 --network 参数（NETWORK_NAME 为空时不加）
                    NETWORK_ARG=""
                    if [ -n "${params.NETWORK_NAME}" ]; then
                        NETWORK_ARG="--network=${params.NETWORK_NAME}"
                        echo "==> joining network: ${params.NETWORK_NAME}"
                    else
                        echo "!! NETWORK_NAME 为空，后端容器会跑在默认 bridge，可能连不上 nacos"
                    fi

                    cat > /tmp/backend-env-\$\$.env <<EOF
SPRING_PROFILES_ACTIVE=${params.PROFILE}
SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=${params.NACOS_HOST}:${params.NACOS_PORT}
SPRING_CLOUD_NACOS_CONFIG_USERNAME=${params.NACOS_USER}
SPRING_CLOUD_NACOS_CONFIG_PASSWORD=${params.NACOS_PASSWORD}
SPRING_CLOUD_NACOS_CONFIG_NAMESPACE=
SPRING_CLOUD_NACOS_CONFIG_GROUP=DEFAULT_GROUP
SPRING_CLOUD_NACOS_CONFIG_FILE_EXTENSION=yaml
TZ=Asia/Shanghai
EOF
                    chmod 600 /tmp/backend-env-\$\$.env

                    docker rm -f ${params.BACKEND_NAME} 2>/dev/null || true
                    docker run -d \\
                        --name ${params.BACKEND_NAME} \\
                        -p ${params.BACKEND_PORT}:8888 \\
                        --restart unless-stopped \\
                        \$NETWORK_ARG \\
                        --env-file /tmp/backend-env-\$\$.env \\
                        ${BACKEND_IMAGE}:${params.BACKEND_TAG}

                    rm -f /tmp/backend-env-\$\$.env

                    # 确认 backend 真的在这个网络里
                    if [ -n "${params.NETWORK_NAME}" ]; then
                        echo '==> backend network:'
                        docker inspect -f '{{.Name}} -> {{json .NetworkSettings.Networks}}' ${params.BACKEND_NAME}
                    fi

                    echo '==> waiting for backend...'
                    for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
                        sleep 3
                        if docker exec ${params.BACKEND_NAME} sh -c 'curl -fsS http://localhost:8888/api-docs' >/dev/null 2>&1; then
                            echo "==> backend ready after \${i}*3s"
                            exit 0
                        fi
                        echo "==> still waiting (\\\$((\$i*3))s)..."
                    done
                    echo '==> backend not ready in 45s; check: docker logs ${params.BACKEND_NAME}'
                    exit 1
                """
            }
        }
    }

    post {
        success {
            echo "✅ deploy OK."
            echo "   nacos:   http://localhost:${params.NACOS_PORT}/nacos  (user=${params.NACOS_USER})"
            echo "   backend: docker logs -f ${params.BACKEND_NAME}  (port ${params.BACKEND_PORT}, profile=${params.PROFILE})"
            echo "   test:    curl http://localhost:${params.BACKEND_PORT}/api-docs"
        }
        failure {
            echo "❌ deploy failed. debug:"
            echo "   docker ps -a | grep ${params.BACKEND_NAME}"
            echo "   docker logs ${params.BACKEND_NAME}"
        }
    }
}
