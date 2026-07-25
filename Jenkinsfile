// ============================================================
// Jenkinsfile — 部署 Backend 到本机 Docker
//   - 假设 backend / mysql / jenkins / nacos 已在同一 docker user-defined 网络里
//   - 容器之间用「容器名」互通（Docker 内置 DNS）
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

        // Nacos（同 docker 网络，直接用容器名；默认可选，连接失败仍继续部署）
        string(name: 'NACOS_HOST',    defaultValue: 'nacos',   description: 'Nacos 主机名')
        string(name: 'NACOS_PORT',    defaultValue: '8848',    description: 'Nacos HTTP 端口')
        string(name: 'NACOS_USER',    defaultValue: 'nacosadmin', description: 'Nacos 用户名')
        password(name: 'NACOS_PASSWORD', defaultValue: 'zVndnMGgkytNH7V0iJg1eqc1hwcTSq9', description: 'Nacos 密码（请使用 Jenkins 凭据/参数注入）')
        booleanParam(name: 'STRICT_NACOS', defaultValue: false, description: 'Nacos 不可用时是否终止部署')

        // prod 脱离 Nacos 启动所需配置
        string(name: 'DB_URL', defaultValue: 'jdbc:mysql://mysql:3306/project_info_manage?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8', description: 'JDBC URL')
        string(name: 'DB_USERNAME', defaultValue: 'root', description: '数据库用户名')
        password(name: 'DB_PASSWORD', defaultValue: 'Root@123456', description: '数据库密码（请使用 Jenkins 凭据/参数注入）')
        password(name: 'JWT_SECRET', defaultValue: 'YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==', description: 'JWT Base64 密钥（请使用 Jenkins 凭据/参数注入）')
        string(name: 'JWT_EXPIRATION', defaultValue: '86400000', description: 'JWT 过期时间（毫秒）')

        // 所有服务所在的 docker 网络（docker network ls 查看）。缺省 deploy-net；
        // Jenkins 在"Build with Parameters"里若被清空,这里兜底回 deploy-net
        string(name: 'NETWORK_NAME',  defaultValue: 'deploy-net', description: 'docker 网络名(留空回退到 deploy-net)')

        // 配置发布（可选）
        string(name: 'CONFIG_REPO_DIR', defaultValue: '',      description: '本地 YAML 目录，留空跳过')
    }

    environment {
        BACKEND_IMAGE = 'leiw-go/back'
    }

    options {
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {

        stage('1. Check Nacos') {
            steps {
                script {
                    // 默认 STRICT_NACOS=false：Nacos 不可达仅告警，继续构建/部署。
                    try {
                        sh '''
                            set +x
                            bash scripts/check-nacos.sh \
                                "$NACOS_HOST" "$NACOS_PORT" "$NACOS_USER" "$NACOS_PASSWORD"
                        '''
                        env.NACOS_STATUS = 'OK'
                    } catch (Exception e) {
                        if (params.STRICT_NACOS) {
                            error("Nacos unreachable and STRICT_NACOS=true: ${e.message}")
                        }
                        echo "⚠️ Nacos unreachable or auth failed: ${e.message}"
                        echo "   backend will start with env-provided DB/JWT only"
                        env.NACOS_STATUS = 'UNREACHABLE'
                    }
                }
            }
        }

        stage('2. Publish Config to Nacos') {
            when { expression { return env.NACOS_STATUS == 'OK' && params.CONFIG_REPO_DIR?.trim() } }
            steps {
                sh '''
                    set +x
                    bash scripts/publish-config.sh \
                        "$NACOS_HOST" "$NACOS_PORT" "$NACOS_USER" "$NACOS_PASSWORD" \
                        "$PROFILE" "$CONFIG_REPO_DIR"
                '''
            }
        }

        stage('3. Build Backend Image') {
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

        stage('4. Deploy Backend Container') {
            steps {
                sh '''
                    set +x
                    # 兜底:如果 NETWORK_NAME 被清空,回退到 deploy-net(deploy-backend.sh 内部还会再兜一次)
                    : "${NETWORK_NAME:=deploy-net}"
                    bash scripts/deploy-backend.sh \
                        "$BACKEND_NAME" "$BACKEND_PORT" "$PROFILE" \
                        "$NACOS_HOST" "$NACOS_PORT" "$NACOS_USER" "$NACOS_PASSWORD" \
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
            echo "   nacos:   http://${params.NACOS_HOST}:${params.NACOS_PORT}/nacos"
            echo "   backend: docker logs -f ${params.BACKEND_NAME}  (port ${params.BACKEND_PORT})"
            echo "   test:    curl http://localhost:${params.BACKEND_PORT}/api-docs"
        }
        failure {
            echo "❌ deploy failed. debug: docker logs ${params.BACKEND_NAME}"
        }
    }
}
