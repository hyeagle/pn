# ===== 编译阶段 =====
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
# 先复制 pom.xml，利用 Docker 缓存层下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B
# 复制源码并编译打包
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
# 从编译阶段拷贝打包好的 jar
COPY --from=builder /build/target/mqtt_postgres.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
