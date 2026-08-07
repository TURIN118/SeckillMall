# ============================================================
# seckill-mall Dockerfile
# 安全修复 B1/M-K3：
#   1. 私钥不再烘焙进镜像，运行时通过 volume 挂载到 /app/keys/
#   2. 容器以非 root 用户 app 运行
#   3. 使用 exec 形式 ENTRYPOINT 以正确转发信号（优雅停机）
#   4. 增加 HEALTHCHECK
# ============================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
# 构建期不需要真实密钥，使用占位密钥（生产密钥运行时挂载覆盖）
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# 创建非 root 用户 app
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# 私钥挂载点：生产通过 -v /host/keys:/app/keys:ro 挂载
RUN mkdir -p /app/keys && chown -R app:app /app
VOLUME ["/app/keys"]

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod
# 私钥路径指向挂载点（覆盖 application.yml 默认 classpath 路径）
ENV JWT_RSA_PRIVATE_KEY_PATH=/app/keys/private.pem
ENV JWT_RSA_PUBLIC_KEY_PATH=/app/keys/public.pem

USER app

# exec 形式：java 进程 PID=1，正确接收 SIGTERM 优雅停机
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# 健康检查：每 30s 探测 actuator/health，5s 超时，连续 3 次失败标记 unhealthy
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
