# Wardrobe

云衣橱商城是一个服装电商全栈项目，包含用户端、管理端、后端服务、AI 推荐、异步订单处理、缓存、安全防护和监控体系。项目重点不只在 CRUD，而是把一个常规电商业务做成可部署、可观测、可扩展的工程化系统。

## 项目简介

系统面向服装电商场景，用户端支持商品浏览、搜索、购物车、下单、订单和个人中心；管理端支持商品、订单、用户、分类、尺码、品牌和统计管理。后端在基础业务之上加入 Redis、RabbitMQ、pgvector、Spring AI、限流、幂等、XSS 过滤和 Prometheus 指标。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 21, Spring Boot 3.5, MyBatis-Plus, Sa-Token, Undertow |
| AI 能力 | Spring AI, PostgreSQL, pgvector, Mock Embedding Service |
| 中间件 | MySQL 8, Redis 7, RabbitMQ |
| 前端 | Vue 3, TypeScript, Vite, Pinia, Element Plus, Axios |
| 安全与稳定性 | BCrypt, 登录鉴权, 接口限流, 幂等拦截, XSS 过滤, 全局异常处理 |
| 可观测性 | Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| 部署 | Docker Compose, Nginx |

## 核心功能

### 用户端

- 商品列表、商品详情、分类筛选和搜索。
- 用户注册、登录、个人资料维护。
- 购物车、结算、下单和订单查看。
- 商品评论、评论展示和个人订单管理。

### 管理端

- 商品、分类、尺码、品牌和库存管理。
- 用户管理、订单管理和评论管理。
- 数据统计与运营看板。

### 后端工程能力

- 使用 Sa-Token 管理登录态和权限。
- 使用 BCrypt 加密用户密码。
- 使用 Redis 缓存热点数据，并通过分布式锁保护库存扣减。
- 使用 RabbitMQ 异步处理订单消息，包含生产者、消费者和死信消费者。
- 使用自定义注解与拦截器实现接口幂等和限流。
- 使用 XSS Filter 过滤恶意输入。
- 使用 Spring AI + pgvector 实现以文搜衣、相似商品推荐和商品描述生成。
- 使用 Actuator + Prometheus + Grafana 暴露 JVM、HTTP 和服务运行指标。

## 架构概览

```text
Vue 用户端 / Vue 管理端
        |
        | HTTP
        v
Spring Boot Backend
  |-- User / Product / Cart / Order / Review
  |-- Auth / RateLimit / Idempotent / XSS
  |-- AI Search / Recommendation / Description
  |-- MQ Producer / Consumer / Dead Letter
        |
        |---------------------------------
        |          |          |          |
      MySQL      Redis     RabbitMQ   PostgreSQL + pgvector
   业务数据     缓存/锁    异步订单       向量检索
        |
   Prometheus + Grafana
```

## 目录结构

```text
wardrobe/
├── wardrobe-backend/       # Spring Boot 后端
├── wardrobe-front-user/    # 用户端 Vue 应用
├── wardrobe-front-admin/   # 管理端 Vue 应用
├── mock-embedding/         # 本地 Mock Embedding 服务
├── monitoring/             # Prometheus 和 Grafana 配置
├── docker-compose.yml      # 本地完整环境编排
├── wardrobe.sql            # MySQL 初始化脚本
├── 安全漏洞.md             # 安全加固记录
└── handoff.md              # 项目交接记录
```

## 快速启动

环境要求：

- JDK 21
- Node.js 18+
- Docker Desktop
- Docker Compose v2

使用 Docker Compose 启动完整环境：

```powershell
cd D:\WebProject\wardrobe
docker compose up -d
docker compose ps
```

主要访问地址：

| 服务 | 地址 |
| --- | --- |
| 用户端 | http://localhost:7070 |
| 管理端 | http://localhost:7087 |
| 后端 API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 |
| RabbitMQ 管理台 | http://localhost:15672 |
| Mock Embedding | http://localhost:3000 |

本地前端开发：

```powershell
cd D:\WebProject\wardrobe\wardrobe-front-user
npm install
npm run dev

cd D:\WebProject\wardrobe\wardrobe-front-admin
npm install
npm run dev
```

后端本地开发：

```powershell
cd D:\WebProject\wardrobe\wardrobe-backend
..\mvnw.cmd test
..\mvnw.cmd spring-boot:run
```

## 配置与密钥

后端配置按运行环境分为三类：

| Profile | 文件 | 用途 |
| --- | --- | --- |
| 默认配置 | `wardrobe-backend/src/main/resources/application.yml` | IDEA 本地开发，连接 `localhost` 暴露的 MySQL、Redis、RabbitMQ 和 pgvector。 |
| `docker` | `wardrobe-backend/src/main/resources/application-docker.yml` | Docker Compose 内部服务互联，服务名使用 `mysql`、`redis`、`rabbitmq`、`postgres`。 |
| `prod` | `wardrobe-backend/src/main/resources/application-prod.yml` | 生产部署，数据库密码、Redis 密码、RabbitMQ 密码、OpenAI API Key、CSRF 域名等必须由环境变量注入。 |

生产环境不要使用仓库里的默认密码或 demo key。建议通过 CI/CD Secret、云厂商 Secret Manager、Kubernetes Secret、Nacos/Apollo 等配置中心注入，例如：

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://prod-mysql:3306/wardrobe?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$env:SPRING_DATASOURCE_USERNAME="wardrobe_app"
$env:SPRING_DATASOURCE_PASSWORD="<from-secret-manager>"
$env:SPRING_REDIS_HOST="prod-redis"
$env:SPRING_REDIS_PASSWORD="<from-secret-manager>"
$env:SPRING_RABBITMQ_HOST="prod-rabbitmq"
$env:SPRING_RABBITMQ_USERNAME="wardrobe_app"
$env:SPRING_RABBITMQ_PASSWORD="<from-secret-manager>"
$env:SPRING_PGVECTOR_URL="jdbc:postgresql://prod-postgres:5432/wardrobe_vec"
$env:SPRING_PGVECTOR_USERNAME="wardrobe_vec_app"
$env:SPRING_PGVECTOR_PASSWORD="<from-secret-manager>"
$env:SPRING_AI_OPENAI_BASE_URL="https://api.openai.com"
$env:SPRING_AI_OPENAI_API_KEY="<from-secret-manager>"
$env:SA_TOKEN_CSRF_ALLOW_DOMAINS="www.example.com,admin.example.com"
..\mvnw.cmd spring-boot:run
```

## 核心接口示例

商品语义搜索：

```http
POST /vector/search
Content-Type: application/json

{
  "query": "夏天 纯棉 蓝色 T恤",
  "topK": 5
}
```

相似商品推荐：

```http
GET /vector/recommend?clothId=1&topK=5
```

AI 商品描述生成：

```http
POST /ai/generate-description
Content-Type: application/json

{
  "clothName": "夏季纯棉T恤",
  "typeName": "T恤",
  "style": "简约",
  "brand": "优衣库"
}
```

健康检查：

```http
GET /actuator/health
```

Prometheus 指标：

```http
GET /actuator/prometheus
```

## 可观测性

后端每个 HTTP 请求都会通过 `TraceIdFilter` 设置 `traceId`：

- 如果请求头带 `X-Trace-Id`，后端会校验后复用该值。
- 如果请求头没有 `X-Trace-Id`，后端会生成新的 traceId。
- 响应头会返回同一个 `X-Trace-Id`，异常响应体也会包含 `traceId`，便于前端报错后定位日志。

本地默认文本日志会带 `traceId`；`docker` 和 `prod` profile 使用 Spring Boot 结构化 JSON 日志，MDC 中的 `traceId` 会随日志一起输出，方便接入 ELK、Loki 或云日志平台。

Prometheus 告警规则位于 `monitoring/alerts/wardrobe-backend-alerts.yml`，覆盖后端不可用、5xx 错误率升高、平均延迟升高、JVM 堆内存过高、CPU 过高和 JDBC 连接池接近耗尽。

## 慢 SQL 分析

项目使用 Druid `StatFilter` 记录慢 SQL：

| 环境 | 阈值 | 说明 |
| --- | --- | --- |
| 本地默认配置 | 500ms | 方便开发阶段尽早发现慢查询。 |
| `docker` | 默认 1000ms | 可通过 `SPRING_DATASOURCE_DRUID_SLOW_SQL_MILLIS` 调整。 |
| `prod` | 默认 1000ms | 输出到结构化日志，便于云日志、ELK 或 Loki 检索。 |

当前 SQL 脚本已为高频查询补充索引：

| 表 | 索引 | 对应查询 |
| --- | --- | --- |
| `t_user` | `uk_user_user_name`, `uk_user_phone`, `idx_user_role` | 登录、注册查重、后台用户列表 |
| `t_order` | `idx_order_user`, `idx_order_status`, `idx_order_user_status` | 用户订单、按状态查订单、用户订单状态筛选 |
| `t_cart` | `idx_cart_user`, `idx_cart_user_cloth_size` | 购物车列表、购物车商品去重 |
| `t_clothes` | `idx_clothes_type`, `idx_clothes_style` | 分类筛选、风格筛选 |
| `t_size` | `idx_size_type` | 按商品类型加载尺码 |

本地验证索引是否命中的示例：

```sql
EXPLAIN SELECT * FROM t_order WHERE user_id = 8 AND status = 0;
EXPLAIN SELECT * FROM t_cart WHERE user_id = 8 AND cloth_id = 1 AND cloth_size = 'S';
EXPLAIN SELECT id, user_name AS userName, password, phone, address, role FROM t_user WHERE phone = '13122222222';
```

`LIKE CONCAT('%', keyword, '%')` 这类包含前缀通配符的搜索通常无法有效使用普通 BTree 索引；如果商品和品牌搜索数据量继续增长，建议升级为 MySQL FULLTEXT、Elasticsearch/OpenSearch，或复用当前 pgvector 语义检索能力。

## 压测报告

压测报告位于 `docs/performance-test-report.md`，压测脚本位于 `scripts/load-test/wardrobe-smoke-load.js`。当前报告提供读接口烟雾压测方案、通过标准、Prometheus 观察项和结果记录表，执行命令：

```powershell
cd D:\WebProject\wardrobe
k6 run .\scripts\load-test\wardrobe-smoke-load.js
```

## 测试

后端包含用户、库存、订单一致性、权限边界、MQ 重试、AI 生成和向量推荐相关测试：

| 测试类 | 覆盖重点 |
| --- | --- |
| `AdminWriteAccessAnnotationTest` | 商品、品牌、分类、尺码 Controller 的公开读与管理员写权限边界 |
| `DataSourceConfigTest` | Druid 慢 SQL 统计过滤器配置 |
| `OrderCreationIntegrationTest` | 订单创建、Redis 预扣库存、Outbox 事件和消息投递的集成链路 |
| `OrderServiceConcurrencyTest` | 同一用户并发下单时的分布式锁保护 |
| `PrometheusMonitoringConfigTest` | Prometheus 告警规则配置 |
| `StockConcurrencyTest` | Redis Lua 并发扣库存，防止超卖 |
| `OrderMessageConsumerTest` | RabbitMQ 消费失败、重试计数和死信流转 |
| `OrderOutboxServiceTest` | Outbox 投递成功、失败重试状态流转 |
| `TraceIdFilterTest` | 请求 traceId 透传、生成和 MDC 清理 |
| `UserServiceTest` | 登录、注册、密码加密和用户删除 |
| `StockServiceTest` | 库存扣减和库存恢复 |
| `ClothesEmbeddingServiceTest` | 向量搜索、相似推荐和降级搜索 |
| `AiGenerationServiceTest` | 商品描述生成和评论摘要 |

运行测试：

```powershell
cd D:\WebProject\wardrobe\wardrobe-backend
mvn test
```

## 简历描述参考

AI 智能服装电商平台：

- 实现用户端和管理端，覆盖商品浏览、搜索、购物车、下单、订单管理、评论和用户管理等电商核心流程。
- 基于 RabbitMQ 实现订单异步处理和库存扣减，结合 Redis 分布式锁、接口幂等和限流机制提升并发场景稳定性。
- 基于 Spring AI 与 pgvector 实现以文搜衣、相似商品推荐和商品描述生成能力。
- 完成 BCrypt 密码加密、Sa-Token 登录鉴权、XSS 过滤、CSRF/限流拦截等安全加固。
- 使用 Prometheus + Grafana 监控 JVM、HTTP 请求和服务运行指标，并通过 Docker Compose 编排完整开发环境。

## 编码说明

项目文档统一使用 UTF-8。Windows PowerShell 查看中文 Markdown 时建议指定编码：

```powershell
Get-Content .\README.md -Encoding UTF8
```
