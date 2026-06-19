# syntax=docker/dockerfile:1.4
# ============================================================
# Spring Boot 后端 - 多阶段构建
#   build:  maven:3.9-eclipse-temurin-17 编译并打包
#   runtime: eclipse-temurin:17-jre-alpine  运行 fat-jar
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先 copy pom 拉依赖（利用 Docker 层缓存）
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline || true

# 再 copy 全部源码并打包
COPY src ./src
RUN mvn -B -DskipTests package \
    && cp target/ProjectInfomationManage.jar /app.jar

# ------------------------------------------------------------
# Runtime 阶段
# ------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app
COPY --from=build /app.jar /app/app.jar

EXPOSE 8888

ENTRYPOINT ["java","-jar","/app/app.jar"]