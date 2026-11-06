// ============================================================
// Jenkinsfile — 部署 Backend 到本机 Docker
//   - 假设 backend / mysql / jenkins 已在同一 docker user-defined 网络里
//   - 容器之间用「容器名」互通（Docker 内置 DNS）
//   - 配置全部来自仓库内 application-{profile}.yml + 环境变量注入，无外部配置中心
//   - 实际逻辑都在 scripts/*.sh 里，本文件只负责串流程
// ============================================================
pipeline {
    agent any

    parameters {
        string(name: 'BACKEND_TAG',   defaultValue: 'latest',  description: '镜像 tag')
        booleanParam(name: 'CLEAN_IMAGE', defaultValue: false, description: '构建前清理同名旧镜像')
        string(name: 'PROFILE',       defaultValue: 'prod',     description: 'Spring profile (dev|prod)')

        // 后端容器
        string(name: 'BACKEND_PORT',  defaultValue: '8888',    description: '后端宿主机端口')
        string(name: 'BACKEND_NAME',  defaultValue: 'backend', description: '后端容器名')

        // 后端启动所需配置（由环境变量注入容器）
        string(name: 'DB_URL', defaultValue: 'jdbc:mysql://mysql:3306/project_info_manage?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8', description: 'JDBC URL')
        string(name: 'DB_USERNAME', defaultValue: 'root', description: '数据库用户名')
        password(name: 'DB_PASSWORD', defaultValue: 'Root@123456', description: '数据库密码（请使用 Jenkins 凭据/参数注入）')
        password(name: 'JWT_SECRET', defaultValue: 'YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==', description: 'JWT Base64 密钥（请使用 Jenkins 凭据/参数注入）')
        string(name: 'JWT_EXPIRATION', defaultValue: '86400000', description: 'JWT 过期时间（毫秒）')

        // 所有服务所在的 docker 网络（docker network ls 查看）。缺省 deploy-net；
        // Jenkins 在"Build with Parameters"里若被清空,这里兜底回 deploy-net
        string(name: 'NETWORK_NAME',  defaultValue: 'deploy-net', description: 'docker 网络名(留空回退到 deploy-net)')

        // Flyway：首次接管存量生产库时需置 true，把已有库登记为 baseline 版本。
        // 之后每次部署都会 validate + 执行未跑过的迁移，此参数可恢复为 false。
        booleanParam(name: 'FLYWAY_BASELINE_ON_MIGRATE', defaultValue: false, description: 'Flyway 首次接管存量库时开启 baseline')
    }

    environment {
        BACKEND_IMAGE = 'leiw-go/back'
    }

    options {
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {

        stage('1. Build Backend Image') {
            steps {
                // 探测可用的 docker 镜像源（规避单一源挂掉导致 build 全失败）
                // env.* 赋值 / if 判定都属任意 Groovy,必须包在 script 块里
                script {
                    env.DOCKER_MIRROR = sh(
                        returnStdout: true,
                        script: 'bash ./scripts/pick-docker-mirror.sh'
                    ).trim()
                    echo "🔍 picked DOCKER_MIRROR=${env.DOCKER_MIRROR}"

                    if (params.CLEAN_IMAGE) {
                        sh "docker rmi -f ${BACKEND_IMAGE}:${params.BACKEND_TAG} || true"
                    }
                }
                sh "docker build --build-arg DOCKER_MIRROR=${env.DOCKER_MIRROR} -f Dockerfile -t ${BACKEND_IMAGE}:${params.BACKEND_TAG} ."
            }
        }

        stage('2. Deploy Backend Container') {
            steps {
                sh '''
                    set +x
                    # 兜底:如果 NETWORK_NAME 被清空,回退到 deploy-net(deploy-backend.sh 内部还会再兜一次)
                    : "${NETWORK_NAME:=deploy-net}"
                    # Flyway 开关透传给容器（prod 首次接管存量库时需为 true）
                    export FLYWAY_BASELINE_ON_MIGRATE="${FLYWAY_BASELINE_ON_MIGRATE:-false}"
                    bash scripts/deploy-backend.sh \
                        "$BACKEND_NAME" "$BACKEND_PORT" "$PROFILE" \
                        "$NETWORK_NAME" "${BACKEND_IMAGE}:${BACKEND_TAG}" \
                        "$DB_URL" "$DB_USERNAME" "$DB_PASSWORD" \
                        "$JWT_SECRET" "$JWT_EXPIRATION"
                '''
            }
        }
    }

    post {
        success {
            echo "✅ deploy OK"
            echo "   backend: docker logs -f ${params.BACKEND_NAME}  (port ${params.BACKEND_PORT})"
            echo "   test:    curl http://localhost:${params.BACKEND_PORT}/api-docs"
        }
        failure {
            echo "❌ deploy failed. debug: docker logs ${params.BACKEND_NAME}"
        }
    }
}
