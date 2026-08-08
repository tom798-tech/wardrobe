# =============================================================================
# wardrobe-backend Dockerfile (多阶段构建)
#   Stage 1: Maven 构建 (使用 JDK 21 + 官方 Maven 镜像，复用依赖层)
#   Stage 2: 运行时  (使用 JDK 21 精简镜像，只拷贝打包好的 jar)
# =============================================================================

# --------------------------- Stage 1: Build ---------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# 写入国内 Maven 镜像 settings.xml (镜像加速，避免从 repo.maven.apache.org 拉)
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
                              http://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*,!spring-milestones</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
    <mirror>
      <id>huaweicloud</id>
      <mirrorOf>spring-milestones</mirrorOf>
      <name>华为云中央仓(托管Spring里程碑)</name>
      <url>https://repo.huaweicloud.com/repository/maven</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>wardrobe-fast</id>
      <activation><activeByDefault>true</activeByDefault></activation>
      <repositories>
        <repository>
          <id>aliyunmaven</id>
          <url>https://maven.aliyun.com/repository/public</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
          <id>spring-milestones</id>
          <url>https://repo.huaweicloud.com/repository/maven</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
          <id>springdoc-releases</id>
          <url>https://repo.spring.io/maven-release</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>aliyunplugins</id>
          <url>https://maven.aliyun.com/repository/public</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
</settings>
EOF

# 先只拷贝 pom.xml，利用 Docker 层缓存加速依赖下载
COPY pom.xml ./pom.xml

# 仅拉取编译/打包需要的依赖和插件，不拉 go-offline 会触发的 site/javadoc/doxia/flexmark 等无用依赖
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:resolve dependency:resolve-plugins -B

# 再拷贝源码执行构建
COPY src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn clean package \
        -DskipTests \
        -Dmaven.site.skip=true \
        -Dmaven.javadoc.skip=true \
        -Dmaven.source.skip=true \
        -Dcheckstyle.skip=true \
        -Dpmd.skip=true \
        -Dspotbugs.skip=true \
        -B

# --------------------------- Stage 2: Runtime -------------------------------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 健康检查 & 时区工具 (healthcheck 用 wget)
ENV DEBIAN_FRONTEND=noninteractive
# 使用阿里云 Ubuntu 镜像 + 清华源双保险加速 apt-get，以及换时区工具
RUN sed -i 's|http://archive.ubuntu.com/ubuntu/|http://mirrors.aliyun.com/ubuntu/|g' /etc/apt/sources.list \
    && sed -i 's|http://security.ubuntu.com/ubuntu/|http://mirrors.aliyun.com/ubuntu/|g' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates tzdata \
    && rm -rf /var/lib/apt/lists/*

# 拷贝上一阶段打好的 jar（名字与 pom 中的 artifact/version 保持一致）
COPY --from=builder /workspace/target/wardrobe_springboot-0.0.1-SNAPSHOT.jar app.jar

# 容器时区设置为东八区
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

# 上传目录 (docker-compose 会挂载 wardrobe-images 到此路径)
RUN mkdir -p /app/images

# 默认使用 docker profile (application-docker.yml 会覆盖数据库/Redis 主机名)
ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
