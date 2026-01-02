# ============================================
# 企业级 SpringBoot Dockerfile
# 多阶段构建 + 安全优化 + 生产就绪
# ============================================

# ---------- 阶段1: 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 先复制 pom.xml，利用 Docker 缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- 阶段2: 运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="bebopze" \
      version="1.0.0" \
      description="TDX Quant Application"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

# 创建非 root 用户（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /tdx-quant

# 从构建阶段复制 JAR
COPY --from=builder /build/target/*.jar tdx-quant.jar

# 创建日志目录并设置权限
RUN mkdir -p /tdx-quant/logs /tdx-quant/config \
    && chown -R appuser:appgroup /tdx-quant

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 7001

# JVM 优化参数
ENV JAVA_OPTS="-Xms8g -Xmx16g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tdx-quant/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:7001/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar tdx-quant.jar"]