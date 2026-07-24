# syntax=docker/dockerfile:1.4
# ============================================================
# Spring Boot 后端 - 多阶段构建（基础镜像走阿里云，国内拉取稳）
#   build:  registry.cn-hangzhou.aliyuncs.com/library/maven:3.9-eclipse-temurin-17
#   runtime: registry.cn-hangzhou.aliyuncs.com/library/eclipse-temurin:17-jre-alpine
# ============================================================
FROM registry.cn-hangzhou.aliyuncs.com/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 让 Maven 走项目里的 settings.xml（华为云镜像，避免 Maven Central 直连）
COPY settings.xml /root/.m2/settings.xml

# 先 copy pom 拉依赖（利用 Docker 层缓存 + BuildKit cache mount 复用 .m2）
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    --mount=type=cache,target=/build/target \
    mvn -B -DskipTests dependency:resolve dependency:resolve-plugins

# 再 copy 全部源码并打包（target/ 也走 cache mount，二次构建只编增量）
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    --mount=type=cache,target=/build/target \
    mvn -B -T 1C -DskipTests package \
    && cp target/ProjectInfomationManage.jar /app.jar

# ------------------------------------------------------------
# Runtime 阶段
# ------------------------------------------------------------
FROM registry.cn-hangzhou.aliyuncs.com/library/eclipse-temurin:17-jre-alpine

# JVM 直接读 TZ 环境变量，省掉 tzdata 包 + 软链接 + 时区文件
ENV TZ=Asia/Shanghai

# 替换 Alpine apk 源为清华镜像（国内拉包稳）
RUN sed -i 's|dl-cdn.alpinelinux.org|mirrors.tuna.tsinghua.edu.cn|g' /etc/apk/repositories \
    && apk add --no-cache curl

WORKDIR /app
COPY --from=build /app.jar /app/app.jar

EXPOSE 8888

ENTRYPOINT ["java","-jar","/app/app.jar"]