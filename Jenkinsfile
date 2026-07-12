// ============================================================
// Jenkinsfile — 部署到本机 Docker (Nacos + Spring Cloud Alibaba 版)
//   - 通过 SSH 拉取 github.com/leiw-go/back.git
//   - 启动 Nacos（MySQL 模式，端口 8848/9848）并 bootstrap schema
//   - 把 config-repo/ProjectInfomationManage-<profile>.yml 推送到 Nacos
//   - 构建 + 部署 Backend（端口 8888，从 Nacos 拉配置）
//   - 敏感配置（DB 密码、JWT secret）由 Nacos 提供，不再写在 Jenkinsfile
// ============================================================
pipeline {
    agent any

    parameters {
        string(name: 'BACKEND_TAG',  defaultValue: 'latest',  description: '镜像 tag')
        booleanParam(name: 'CLEAN_IMAGE', defaultValue: false, description: '构建前是否清理同名旧镜像')
        string(name: 'PROFILE',      defaultValue: 'dev',     description: 'Spring profile (dev|prod)')
        string(name: 'BACKEND_PORT', defaultValue: '8888',    description: '后端宿主机暴露端口')
        string(name: 'BACKEND_NAME', defaultValue: 'backend', description: '后端容器名')
        string(name: 'CONFIG_REPO_DIR', defaultValue: 'C:/Users/Valley/config-repo',
               description: '本地 YAML 配置目录（不入 git，作为 Nacos 的发布源）')
        string(name: 'NACOS_IMAGE', defaultValue: 'docker.m.daocloud.io/nacos/nacos-server:v2.3.2',
               description: 'Nacos 镜像')
        string(name: 'NACOS_CONTAINER', defaultValue: 'nacos', description: 'Nacos 容器名')
        string(name: 'NACOS_PORT', defaultValue: '8848',  description: 'Nacos HTTP 端口')
        string(name: 'NACOS_GRPC_PORT', defaultValue: '9848', description: 'Nacos gRPC 端口（必须暴露）')
        string(name: 'NACOS_USER', defaultValue: 'nacos', description: 'Nacos 用户名')
        string(name: 'NACOS_PASSWORD', defaultValue: 'Valley@Nacos#2026', description: 'Nacos 密码（首次启动后生效）')
        string(name: 'MYSQL_HOST', defaultValue: 'host.docker.internal', description: 'Nacos -> MySQL 的主机')
        string(name: 'MYSQL_PORT', defaultValue: '3306', description: 'MySQL 端口')
        string(name: 'MYSQL_USER', defaultValue: 'root', description: 'MySQL 用户')
        string(name: 'MYSQL_PASSWORD', defaultValue: 'Root@123456', description: 'MySQL 密码')
        string(name: 'MYSQL_DB', defaultValue: 'nacos_config', description: 'Nacos 元数据库')
    }

    environment {
        BACKEND_IMAGE = 'leiw-go/back'
        // Nacos 2.x 要求 token 是 base64 编码字符串（解码后 ≥ 32 字节）
        NACOS_AUTH_TOKEN_VALUE = 'c2VjcmV0a2V5MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6MTIzNDU2Nzg5MA=='
        NACOS_IDENTITY_KEY_VALUE = 'valley-identity'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {

        stage('0. Prep SSH Key') {
            steps {
                sh '''
                    set -eux
                    mkdir -p /tmp/jenkins-ssh
                    cp /var/jenkins_home/.ssh/id_rsa       /tmp/jenkins-ssh/id_rsa
                    cp /var/jenkins_home/.ssh/known_hosts  /tmp/jenkins-ssh/known_hosts
                    chmod 700 /tmp/jenkins-ssh
                    chmod 600 /tmp/jenkins-ssh/id_rsa
                    chmod 644 /tmp/jenkins-ssh/known_hosts
                '''
            }
        }

        stage('1. Deploy Nacos') {
            steps {
                sh """
                    set -eux

                    # ---- 1a. 拉镜像 ----
                    docker pull ${params.NACOS_IMAGE}

                    # ---- 1b. 抽取 Nacos 自带的 mysql-schema.sql ----
                    rm -rf /tmp/nacos-conf && mkdir -p /tmp/nacos-conf
                    docker create --name nacos-conf-tmp ${params.NACOS_IMAGE} >/dev/null
                    EXTRACTED=0
                    for p in /home/nacos/conf/mysql-schema.sql /conf/mysql-schema.sql; do
                      if docker cp nacos-conf-tmp:\${p} /tmp/nacos-conf/mysql-schema.sql 2>/dev/null; then
                        echo "==> extracted schema from \${p}"
                        EXTRACTED=1
                        break
                      fi
                    done
                    docker rm -f nacos-conf-tmp >/dev/null
                    if [ "\${EXTRACTED}" != "1" ]; then
                        echo "!! cannot locate mysql-schema.sql in image"; exit 1
                    fi

                    # ---- 1c. 创建 nacos_config 库（幂等）----
                    docker exec --env MYSQL_PWD=${params.MYSQL_PASSWORD} mysql-simple \\
                        mysql -uroot -e "CREATE DATABASE IF NOT EXISTS ${params.MYSQL_DB} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

                    # ---- 1d. 幂等导入 schema ----
                    if ! docker exec --env MYSQL_PWD=${params.MYSQL_PASSWORD} mysql-simple \\
                            mysql -uroot -e "SHOW TABLES IN ${params.MYSQL_DB}" 2>/dev/null | grep -q config_info; then
                        docker run --rm --network bridge \\
                            -v /tmp/nacos-conf/mysql-schema.sql:/schema.sql \\
                            docker.m.daocloud.io/library/mysql:8.0 \\
                            sh -c "mysql -h${params.MYSQL_HOST} -P${params.MYSQL_PORT} -u${params.MYSQL_USER} -p${params.MYSQL_PASSWORD} ${params.MYSQL_DB} < /schema.sql && echo '==> schema imported'"
                    else
                        echo "==> schema already imported, skip"
                    fi

                    # ---- 1e. 启 Nacos ----
                    docker rm -f ${params.NACOS_CONTAINER} 2>/dev/null || true
                    docker run -d \\
                        --name ${params.NACOS_CONTAINER} \\
                        -p ${params.NACOS_PORT}:8848 \\
                        -p ${params.NACOS_GRPC_PORT}:9848 \\
                        --restart unless-stopped \\
                        -e MODE=standalone \\
                        -e SPRING_DATASOURCE_PLATFORM=mysql \\
                        -e MYSQL_SERVICE_HOST=${params.MYSQL_HOST} \\
                        -e MYSQL_SERVICE_PORT=${params.MYSQL_PORT} \\
                        -e MYSQL_SERVICE_DB_NAME=${params.MYSQL_DB} \\
                        -e MYSQL_SERVICE_USER=${params.MYSQL_USER} \\
                        -e MYSQL_SERVICE_PASSWORD=${params.MYSQL_PASSWORD} \\
                        -e NACOS_AUTH_ENABLE=true \\
                        -e NACOS_AUTH_TOKEN=${env.NACOS_AUTH_TOKEN_VALUE} \\
                        -e NACOS_AUTH_IDENTITY_KEY=${env.NACOS_IDENTITY_KEY_VALUE} \\
                        -e NACOS_AUTH_TOKEN_EXPIRE_SECONDS=18000 \\
                        ${params.NACOS_IMAGE}

                    # ---- 1f. 等待 Nacos 就绪（首次启动 + schema 初始化可能 30-60s）----
                    echo '==> waiting for nacos...'
                    for i in \$(seq 1 40); do
                        sleep 2
                        if curl -fsS http://localhost:${params.NACOS_PORT}/nacos/ >/dev/null 2>&1; then
                            echo "==> nacos ready after \${i}*2s"
                            exit 0
                        fi
                    done
                    echo '==> nacos not ready in 80s; check: docker logs ${params.NACOS_CONTAINER}'
                    exit 1
                """
            }
        }

        stage('2. Publish Config to Nacos') {
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
                sh "docker build -f Dockerfile.host -t ${BACKEND_IMAGE}:${params.BACKEND_TAG} ."
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
SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=host.docker.internal:${params.NACOS_PORT}
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
            echo "   docker ps -a | grep -E '${params.BACKEND_NAME}|${params.NACOS_CONTAINER}'"
            echo "   docker logs ${params.NACOS_CONTAINER}"
            echo "   docker logs ${params.BACKEND_NAME}"
        }
    }
}