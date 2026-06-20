// ============================================================
// Jenkinsfile — 通用版部署到本机 Docker
//   - 从当前仓库 checkout 源码并构建镜像
//   - 所有部署相关参数（DB 地址、端口、镜像 tag）通过参数注入
//   - 部署目标：当前 Jenkins 节点可访问的 Docker daemon
// ============================================================
pipeline {
    agent any

    parameters {
        string(name: 'BACKEND_TAG',  defaultValue: 'latest',  description: '镜像 tag')
        booleanParam(name: 'CLEAN_IMAGE', defaultValue: false, description: '构建前是否清理同名旧镜像')
        string(name: 'BACKEND_PORT', defaultValue: '8888',    description: '宿主机暴露端口')
        string(name: 'BACKEND_NAME', defaultValue: 'backend', description: '容器名')
        string(name: 'DB_HOST',      defaultValue: 'mysql',   description: 'MySQL 主机名（容器内可达的服务名）')
        string(name: 'DB_PORT',      defaultValue: '3306',    description: 'MySQL 端口')
        string(name: 'DB_NAME',      defaultValue: 'project_info_manage', description: '数据库名')
        string(name: 'DB_USER',      defaultValue: 'root',    description: 'DB 用户名')
        password(name: 'DB_PASSWORD', defaultValue: 'Root@123456', description: 'DB 密码（生产请改为凭据）')
    }

    environment {
        BACKEND_IMAGE = 'leiw-go/back'
        // MySQL 8 caching_sha2_password 需要 allowPublicKeyRetrieval
        DB_PARAMS     = '?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {

        stage('1. Build Image') {
            steps {
                script {
                    if (params.CLEAN_IMAGE) {
                        sh "docker rmi -f ${BACKEND_IMAGE}:${params.BACKEND_TAG} || true"
                    }
                }
                sh "docker build -t ${BACKEND_IMAGE}:${params.BACKEND_TAG} ."
                sh "docker images | grep ${BACKEND_IMAGE} || true"
            }
        }

        stage('2. Deploy Container') {
            steps {
                script {
                    def jdbcUrl = "jdbc:mysql://${params.DB_HOST}:${params.DB_PORT}/${params.DB_NAME}${DB_PARAMS}"

                    sh """
                        set -eux
                        # 兜底：确保 deploy-net 网络存在（如果你的环境用其他网络名，改为对应名字）
                        docker network inspect deploy-net >/dev/null 2>&1 || docker network create deploy-net

                        docker rm -f ${params.BACKEND_NAME} || true

                        docker run -d \\
                            --name ${params.BACKEND_NAME} \\
                            --network deploy-net \\
                            --restart unless-stopped \\
                            -p ${params.BACKEND_PORT}:8888 \\
                            -e SPRING_DATASOURCE_URL='${jdbcUrl}' \\
                            -e SPRING_DATASOURCE_USERNAME='${params.DB_USER}' \\
                            -e SPRING_DATASOURCE_PASSWORD='${params.DB_PASSWORD}' \\
                            -e TZ=Asia/Shanghai \\
                            ${BACKEND_IMAGE}:${params.BACKEND_TAG}

                        echo '==> waiting for backend...'
                        for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
                            sleep 3
                            if docker exec ${params.BACKEND_NAME} curl -fsS http://localhost:8888/api-docs >/dev/null 2>&1; then
                                echo "==> backend ready after \${i}*3s"
                                exit 0
                            fi
                            echo "==> still waiting (\$((i*3))s)..."
                        done
                        echo '==> backend not ready in 45s; check: docker logs ${params.BACKEND_NAME}'
                        exit 1
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ deploy OK. image=${env.BACKEND_IMAGE}:${params.BACKEND_TAG}"
            echo "   logs : docker logs -f ${params.BACKEND_NAME}"
            echo "   test : curl http://localhost:${params.BACKEND_PORT}/api-docs"
        }
        failure {
            echo "❌ deploy failed. debug:"
            echo "   docker ps -a | grep ${params.BACKEND_NAME}"
            echo "   docker logs ${params.BACKEND_NAME}"
        }
        always {
            deleteDir()
        }
    }
}