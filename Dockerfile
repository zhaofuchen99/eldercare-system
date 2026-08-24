# =====================================================================
# 后端镜像（Spring Boot 4.1 / Java 25 / Maven 多阶段构建）
# 构建：docker build -t eldercare-backend .
# =====================================================================

# ---- 构建阶段：Maven 打包 ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn -B clean package -DskipTests

# ---- 运行阶段：仅 JRE，镜像更小 ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/eldercare-system-0.0.1-SNAPSHOT.jar /app/app.jar

# 时区：容器默认 UTC，统一 Asia/Shanghai
ENV TZ=Asia/Shanghai
# JVM 内存按容器限额自适应（-m 限制的内存 × 75%）
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
