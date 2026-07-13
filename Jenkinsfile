// ============================================================
// Jenkinsfile — 部署 Backend 到本机 Docker
//   - 假设本机已部署好 Nacos（默认端口 8848 / 9848）
//   - 负责：拉取代码 -> 构建镜像 -> 推送配置到 Nacos -> 启动后端容器
//   - 后端通过 host.docker.internal:8848 访问宿主上的 Nacos
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

        // 本机 Nacos（默认端口已经部署好）
        string(name: 'NACOS_HOST',    defaultValue: 'host.docker.internal',
               description: 'Nacos 主机（容器内视角）')
        string(name: 'NACOS_PORT',    defaultValue: '8848',    description: 'Nacos HTTP 端口')
        string(name: 'NACOS_USER',    defaultValue: 'nacosadmin',   description: 'Nacos 用户名')
        string(name: 'NACOS_PASSWORD', defaultValue: 'zVndnMGgkytNH7V0iJg1eqc1hwcTSq9',  description: 'Nacos 密码')

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
                sh """
                    set -eux
                    echo "==> 验证本机 Nacos (${params.NACOS_HOST}:${params.NACOS_PORT}) 可达"
                    # 优先走 8848（同机部署），build agent 上 localhost 即可
                    for i in \$(seq 1 15); do
                        if curl -fsS http://localhost:${params.NACOS_PORT}/nacos/ >/dev/null 2>&1; then
                            echo "==> nacos ready after \${i} attempts"
                            exit 0
                        fi
                        echo "==> waiting for nacos (\$i/15)..."
                        sleep 2
                    done
                    echo "!! 本机未检测到 Nacos，请先在 ${params.NACOS_PORT} 启动 Nacos"
                    exit 1
                """
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
                    curl -fsS -X POST "http://localhost:${params.NACOS_PORT}/nacos/v1/cs/configs" \\
                        -u '${params.NACOS_USER}:${params.NACOS_PASSWORD}' \\
                        --data-urlencode "dataId=\${DATA_ID}" \\
                        --data-urlencode "group=\${GROUP}" \\
                        --data-urlencode "content@\${FILE}" \\
                        --data-urlencode "type=yaml" \\
                        && echo "==> published \${DATA_ID}"

                    # 读回验证
                    echo '==> verifying...'
                    curl -fsS "http://localhost:${params.NACOS_PORT}/nacos/v1/cs/configs?dataId=\${DATA_ID}&group=\${GROUP}" \\
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
                sh """
                    set -eux
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
                        --add-host=host.docker.internal:host-gateway \\
                        --env-file /tmp/backend-env-\$\$.env \\
                        ${BACKEND_IMAGE}:${params.BACKEND_TAG}

                    rm -f /tmp/backend-env-\$\$.env

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
