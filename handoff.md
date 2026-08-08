# 云衣橱（Wardrobe）商城项目交接文档

> 交接日期：2026-07-26
> 项目代号：wardrobe（衣橱商城：用户端 + 管理端 + 后端）
> 部署方式：Docker Compose 全栈一键启动（12 个容器，全部 healthy）

---

## 一、项目结构

```
D:\WebProject\wardrobe\               ← ✅ 新技术栈统一根目录
├── docker-compose.yml                ← 12 服务编排（必看）
├── wardrobe.sql                      ← MySQL 建库 + 数据（需启动后手动导入，见 3.3 节）
├── README.md                         ← 项目说明文档
│
├── wardrobe-backend\                 ← 后端（Spring Boot 3.5 + Java 21 LTS）
│   ├── Dockerfile                    ← 多阶段构建：Maven 构建 → JRE 运行
│   ├── pom.xml                       ← Undertow 容器、MyBatis-Plus、Sa-Token、Spring AI、Druid、RabbitMQ
│   ├── src\main\java\com\itheima\
│   │   ├── WardrobeApplication.java  ← @SpringBootApplication + @EnableCaching
│   │   ├── controller\               ← 11 个 REST Controller（新增 AiController、ClothesVectorController）
│   │   ├── service\                  ← 11 个 Service（新增 ClothesEmbeddingService、AiGenerationService）
│   │   ├── mapper\                   ← 9 个 MyBatis-Plus Mapper 接口
│   │   ├── entity\                   ← 10 个实体
│   │   ├── config\                   ← VectorStoreConfig、DataSourceConfig、RabbitMQConfig、SaTokenConfig、SecurityConfig、OpenApiConfig等
│   │   ├── mq\                       ← OrderMessageProducer、OrderMessageConsumer
│   │   └── resources\application.yml ← 读 Docker 环境变量
│   └── src\test\java\com\itheima\service\  ← 3 个单元测试类（29个测试方法）
│
├── wardrobe-front-user\              ← 用户端（Vue 3 + Vite 6 + TS 5.6 + Pinia + Element Plus 2.10）
│   ├── Dockerfile                    ← 多阶段构建：npm build → Nginx 托管
│   ├── nginx.conf                    ← charset utf-8; + /api 反代 wardrobe-backend:8080
│   ├── vite.config.ts                ← port 7076（开发），/api → http://localhost:8080
│   └── src\
│       ├── main.ts                   ← 挂载 Element Plus + Pinia + Router
│       ├── router\router.ts          ← history 模式，/user 前缀
│       ├── types\index.ts            ← 全部 TS 类型
│       ├── stores\user.ts            ← Pinia：登录态 + 购物车数量
│       ├── utils\request.ts          ← axios 封装（Sa-Token satoken 自动带）
│       ├── App.vue                   ← 外层布局：品牌栏 + 水平菜单
│       └── components\               ← 12 个页面组件（含 ClothesDetail AI评论分析模块）
│
├── wardrobe-front-admin\             ← 管理端（Vue 3 + Vite 6 + TS 5.6 + Pinia + Element Plus 2.10）
│   ├── Dockerfile                    ← 多阶段构建：npm build → Nginx 托管
│   ├── nginx.conf                    ← charset utf-8; + /api 反代 + rewrite 规则
│   ├── vite.config.ts                ← base: '/'
│   └── src\
│       ├── main.ts / App.vue
│       ├── router.ts
│       ├── utils\request.ts
│       ├── stores\index.ts
│       └── components\               ← Login / Dashboard + 5 个管理页（含 AddClothes AI生成描述按钮）
│
├── mock-embedding\                   ← Mock Embedding 服务（Node.js）
│   ├── server.js                     ← 模拟 Embedding + Chat Completions API
│   └── init-pgvector.sql             ← PostgreSQL 初始化脚本
│
└── prometheus\                       ← Prometheus 配置
    └── prometheus.yml                ← 采集后端指标
```

---

## 二、技术栈一览

| 层 | 技术 | 版本 | 备注 |
|----|------|------|------|
| 后端框架 | **Spring Boot** | 3.5.x | Undertow 容器（非 Tomcat）|
| ORM | **MyBatis-Plus** | 3.5.7 | `@MapperScan("com.itheima.mapper")` + 分页插件 |
| 认证 | **Sa-Token** | 1.38 | `StpUtil.login()`，Cookie 名 `satoken` |
| 缓存 | **Redis** | 7-alpine | Lettuce 客户端，Spring Cache `@Cacheable` 默认 TTL 30 分钟 |
| 连接池 | **Druid** | - | MySQL 数据源 |
| 主数据库 | **MySQL** | 8.0 | 端口映射 3307→3306，账号 root/123456 |
| 向量数据库 | **PostgreSQL + pgvector** | pg16 | 端口映射 5432→5432，支持 1536 维向量余弦相似度搜索 |
| 消息队列 | **RabbitMQ** | 3-management | 端口 5672/15672，订单异步处理 |
| 监控系统 | **Prometheus + Grafana** | 最新 | Prometheus 9090，Grafana 3001，JVM 指标监控 |
| AI框架 | **Spring AI** | 1.0.x | Embedding 模型 + PgVectorStore 向量存储 |
| API文档 | **SpringDoc OpenAPI** | 2.8.9 | Swagger UI：/swagger-ui.html |
| 密码加密 | **BCrypt** | - | Spring Security BCryptPasswordEncoder |
| 单元测试 | **JUnit 5 + Mockito** | - | 3 个测试类，29 个测试方法 |
| 用户端前端 | **Vue 3 + Vite + TS** | Vue 3.5 / Vite 6 / TS 5.6 | Element Plus 2.10 + Pinia 2.3 |
| 管理端前端 | **Vue 3 + Vite + TS** | 同上 | vite base='/' |
| 前端代理 | **Nginx** | alpine | 含 `charset utf-8;`（中文不乱码关键） |
| 容器化 | **Docker Compose** | v2 | 12 服务一键拉起，含 healthcheck + depends_on |

---

## 三、运维手册 ⭐（必看）

### 3.1 启动 / 停止 / 查看命令

```powershell
# ⚠️ 注意：所有命令在 D:\WebProject\wardrobe\ 目录下执行
cd D:\WebProject\wardrobe

# 🟢 第一次启动（构建镜像 + 后台运行 + 强制无缓存）
docker compose build --no-cache
docker compose up -d

# 🟢 日常启动（已有镜像，只拉起容器）
docker compose up -d

# 🟢 只启动基础服务（数据库/消息队列等，后端在IDEA本地开发）
docker compose up -d mysql redis postgres rabbitmq mock-embedding

# 🔴 停止（保留数据卷）
docker compose down

# 🔴 彻底清场（⚠️ 会删除所有数据卷）
docker compose down -v

# 🔍 查看 12 个容器健康状态（healthy 才算就绪）
docker compose ps

# 📜 看后端日志
docker logs -f wardrobe-backend --tail 200

# 📜 看前端 Nginx 日志
docker logs -f wardrobe-front-admin --tail 100
docker logs -f wardrobe-front-user --tail 100
```

### 3.2 12 大服务端口 + 访问地址

| 容器名 / 服务名 | 镜像 | 端口映射 | 访问地址 | 健康检查端点 |
|-----------------|------|----------|----------|--------------|
| **wardrobe-mysql** / mysql | mysql:8.0 | `3307:3306` | 本机 Navicat：`localhost:3307` root/123456 | `mysqladmin ping` |
| **wardrobe-redis** / redis | redis:7-alpine | `6380:6379` | 本机 redis-cli：`-h localhost -p 6380` | `redis-cli ping` |
| **wardrobe-postgres** / postgres | pgvector/pgvector:pg16 | `5432:5432` | 本机 DBeaver：`localhost:5432` postgres/123456 | `pg_isready` |
| **wardrobe-rabbitmq** / rabbitmq | rabbitmq:3-management | `5672:5672` `15672:15672` | 管理后台：`http://localhost:15672` admin/123456 | `rabbitmq-diagnostics ping` |
| **wardrobe-prometheus** / prometheus | prom/prometheus | `9090:9090` | `http://localhost:9090` | HTTP `/-/healthy` |
| **wardrobe-grafana** / grafana | grafana/grafana | `3001:3000` | `http://localhost:3001` admin/admin | HTTP `/api/health` |
| **wardrobe-mock-embedding** / mock-embedding | node:20-alpine | `3000:3000` | `http://localhost:3000/health` | HTTP `/health` |
| **wardrobe-backend** | 本地构建 | `8081:8080` | Docker版后端：`http://localhost:8081` | `actuator/health` → UP |
| **wardrobe-front-user** | 本地构建 | `7070:80` | **用户端：http://localhost:7070/** | HTTP `/` → 200 |
| **wardrobe-front-admin** | 本地构建 | `7087:80` | **管理端：http://localhost:7087/** | HTTP `/` → 200 |

> 💡 IDEA 本地开发后端用 **8080** 端口，Docker 版后端用 **8081** 端口，互不冲突。
>
> ⚠️ 本地端口故意避开本机常见占用（MySQL 3306→3307、Redis 6379→6380）。

### 3.3 首次启动必做：手动导入 wardrobe.sql（解决中文乱码关键）

Docker Compose 不再用 docker-entrypoint-initdb.d 自动导入（会导致中文乱码），**必须等 MySQL 容器 healthy 后手动执行**：

```powershell
# 1. 确认 MySQL healthy（STATUS 列显示 healthy）
docker compose ps mysql

# 2. 建库 + 导入（容器内执行 mysql，指定 --default-character-set=utf8mb4）
docker exec -i wardrobe-mysql `
  mysql -uroot -p123456 --default-character-set=utf8mb4 `
  -e "CREATE DATABASE IF NOT EXISTS wardrobe DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci; USE wardrobe; source /tmp/wardrobe-content.sql;"

# 3. 验证导入成功（t_clothes 有数据）
docker exec wardrobe-mysql mysql -uroot -p123456 -e "USE wardrobe; SELECT COUNT(*) AS cloth_count FROM t_clothes;"
# Expected: 61 件

# 4. ⚠️ 导入 SQL 后，必须清空 Redis 缓存
docker exec wardrobe-redis redis-cli FLUSHALL
```

### 3.4 所有账号密码汇总 🔐

| 系统 | 用户名 | 密码 | 角色/说明 |
|------|--------|------|-----------|
| **管理端**（http://localhost:7087）| `admin` | `admin123` | 超级管理员（role=1） |
| **用户端**（http://localhost:7070）| `zhangsan` | `123123` | 普通用户（role=0） |
| **用户端**（http://localhost:7070）| `lisi` | `123123` | 普通用户 |
| MySQL 容器 | root | `123456` | 端口 3307，库名 wardrobe |
| PostgreSQL 容器 | postgres | `123456` | 端口 5432，库名 wardrobe_vec |
| Redis 容器 | - | （无密码） | 端口 6380 |
| RabbitMQ 管理后台 | admin | `123456` | 端口 15672 |
| Grafana | admin | admin | 端口 3001 |

> 💡 密码说明：数据库中密码已使用 BCrypt 加密存储（$2a$ 开头）。

---

## 四、进度：已完成里程碑（18 项全绿 ✅）

### 4.1 技术栈完成度对比

| 分层 | 目标 | 现状 | 完成度 |
|------|------|------|--------|
| **Java 环境** | JDK21 + Undertow | JDK21 + Undertow | **100%** |
| **后端框架** | SpringBoot3.5 + SpringAI + MyBatis-Plus + Sa-Token + Hutool | 全部已集成 | **100%** |
| **前端** | Vue3 + Vite + TS + Element Plus | Vue3 + Vite 6 + TS 5.6 + Pinia | **100%** |
| **数据存储** | MySQL + PostgreSQL+pgvector + Redis | MySQL + PostgreSQL(pgvector) + Redis | **100%** |
| **消息/监控** | RabbitMQ + Prometheus + Grafana | 全部已部署 | **100%** |
| **部署** | Docker 容器化 | Docker Compose 12 服务编排 | **100%** |
| **代码质量** | 单元测试 + API文档 + 密码安全 | 29个测试 + Swagger + BCrypt | **100%** |

### 4.2 里程碑列表

| # | 里程碑 | 内容摘要 | 关键文件 |
|---|--------|----------|----------|
| 1 | 基础修复 | 管理端 5 大页面报错 + 后端接口补全关联数据 | Controller + Service 各 9 个 |
| 2 | A+B 项目收纳 | 用「复制法」把新旧项目分离，新技术栈全部放 `D:\WebProject\wardrobe\` | 本目录结构 |
| 3 | 后端技术栈切换 | SSM → Spring Boot 3.5 + MyBatis-Plus 3.5.7 + Sa-Token + Undertow | pom.xml + 所有 entity/service/mapper |
| 4 | ⑥ Docker 容器化 | 5 服务编排 + healthcheck + depends_on + 数据卷持久化 | docker-compose.yml |
| 5 | ③ 前端 Vite+TS | 用户端 + 管理端从 Vue-CLI/JS → Vite + TypeScript + Pinia | vite.config.ts、types/index.ts |
| 6 | Docker 全栈验证 | `docker compose up -d` 一条命令全 healthy | docker compose ps |
| 7 | 中文乱码修复 | MySQL utf8mb4 + Nginx charset + 手动导入 + Redis FLUSHALL | nginx.conf、docker-compose.yml |
| 8 | 路由 404 修复 | 用户端「我的订单」等 404：12 条重定向 + name 跳转 | router.ts、App.vue |
| 9 | 购物车 + 下单流程 | DB 补列 + 实体补字段 + 尺码兼容 | Clothes.java、CartController.java |
| 10 | 导航精简 | 删除用户端「分类」「风格」菜单（首页筛选已有） | App.vue、router.ts |
| 11 | 管理端登录验证 | admin/admin123 登录 → 6 张统计卡 + 6 大功能菜单 | Dashboard.vue |
| 12 | ④ 向量数据库集成 | PostgreSQL + pgvector 双数据源、以文搜衣、相似推荐 | VectorStoreConfig.java、ClothesEmbeddingService.java |
| 13 | ⑤ 消息+监控组件 | RabbitMQ 消息队列、Prometheus、Grafana、Actuator | RabbitMQConfig.java、prometheus.yml |
| 14 | AI 功能落地 | AI生成商品描述、AI评论摘要、Mock Embedding服务 | AiGenerationService.java、AiController.java |
| 15 | 密码安全加固 | BCrypt 密码加密存储 + SecurityConfig 配置 | SecurityConfig.java、UserService.java |
| 16 | API 文档 | SpringDoc OpenAPI + Swagger UI | OpenApiConfig.java |
| 17 | 单元测试 | JUnit 5 + Mockito，3个测试类29个测试方法 | src/test/java/... |
| 18 | 项目文档 | README.md 项目说明文档 | README.md |

---

## 五、核心功能说明

### 5.1 AI 智能推荐（向量数据库）

| 功能 | 接口 | 说明 |
|------|------|------|
| 以文搜衣 | `POST /vector/search?keyword=xxx&topK=5` | 自然语言搜索，向量相似度匹配 |
| 相似推荐 | `GET /vector/recommend/{clothesId}?topK=3` | 根据商品ID推荐相似款 |
| 刷新向量库 | `POST /vector/refresh` | 全量刷新商品向量数据 |
| 向量库状态 | `GET /vector/status` | 查看向量库商品数量 |

### 5.2 AI 内容生成

| 功能 | 接口 | 说明 |
|------|------|------|
| 生成商品描述 | `POST /ai/generate-description` | 根据商品属性AI生成描述 |
| 评论摘要分析 | `GET /ai/comment-summary/{clothId}` | 总结好评/差评/综合评价 |

### 5.3 监控指标

| 端点 | 地址 | 说明 |
|------|------|------|
| 健康检查 | `GET /actuator/health` | UP/DOWN |
| 指标数据 | `GET /actuator/prometheus` | Prometheus 格式 |
| 信息 | `GET /actuator/info` | 应用信息 |
| Swagger UI | `GET /swagger-ui.html` | API 文档 |
| OpenAPI JSON | `GET /v3/api-docs` | OpenAPI 3.0 规范 |

---

## 六、开发环境说明

### IDEA 本地开发配置

1. **启动 Docker 基础服务**：
   ```bash
   cd D:\WebProject\wardrobe
   docker compose up -d mysql redis postgres rabbitmq mock-embedding
   ```

2. **IDEA 运行配置**：
   - Run/Debug Configurations → WardrobeApplication
   - VM options：`-Dspring.profiles.active=docker`
   - Working directory：`D:\WebProject\wardrobe\wardrobe-backend`

3. **端口分配**：
   | 服务 | IDEA本地 | Docker版 |
   |------|----------|----------|
   | 后端 | 8080 | 8081 |
   | MySQL | 3307 | 3307 |
   | Redis | 6380 | 6380 |
   | PostgreSQL | 5432 | 5432 |

### 单元测试运行

```bash
cd wardrobe-backend

# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=ClothesEmbeddingServiceTest
mvn test -Dtest=AiGenerationServiceTest
mvn test -Dtest=UserServiceTest
```

---

## 七、踩坑排错手册 ⚠️（18 条血泪教训）

### ❌ 坑 1：Docker 首次构建镜像拉取超时
**修复**：配置阿里云 + DaoCloud 镜像源（Docker Desktop → Settings → Docker Engine）

### ❌ 坑 2：端口冲突（本机装了 MySQL/Redis）
**修复**：已改映射（MySQL 3306→3307、Redis 6379→6380），后端 IDEA 用 8080，Docker 用 8081

### ❌ 坑 3：后端 Maven 构建下载慢
**修复**：Dockerfile 改为 `dependency:resolve` + 内嵌阿里云 Maven 镜像 settings.xml

### ❌ 坑 4：前端 TypeScript 迁移后 "cannot find module"
**修复**：建 `src/types/index.ts` 全定义 + axios 泛型 + 图标用 Goods 代替 Clothes

### ❌ 坑 5：管理端登录页空白 + 资源 404
**根因**：vite.config.ts 里 `base: '/admin/'` 但 Nginx 根目录不对
**修复**：改为 `base: '/'`

### ❌ 坑 6：网页中文全是乱码
**根因链**（必须全部满足）：
1. MySQL 不是 utf8mb4 → 已加 mysqld command 参数
2. 自动导入 SQL 乱码 → 改手动导入，带 `--default-character-set=utf8mb4`
3. Nginx 没加 charset → 两个 nginx.conf 都加了 `charset utf-8;`
4. Redis 缓存旧乱码 → `FLUSHALL` 清空

### ❌ 坑 7：管理端登录按钮点了没反应
**排查**：检查字段名是 `user_name` 不是 `userName`，检查有没有 role=1 的管理员

### ❌ 坑 8：用户端「我的订单」点进去 404
**修复**：12 条重定向兼容 + 所有跳转用 `router.push({ name: 'Order' })`

### ❌ 坑 9：商品详情页按钮永远灰的
**根因链**：t_clothes 缺 stock 等5列 → Clothes.java 没映射 → Size 字段前后端不一致
**修复**：ALTER TABLE 补列 + 实体加字段 + Size.java 加 sizeValue 兼容

### ❌ 坑 10：Lombok @Data 全失效
**根因**：`@TableField(exist=false)` 标在了方法上不是字段上，Lombok 处理异常
**修复**：移到字段声明上

### ❌ 坑 11：创建订单报 Data truncation
**修复**：ALTER TABLE 改 status INT + clothes_details TEXT

### ❌ 坑 12：后端接口全 404 / 前端代理 404
**排查**：直接 curl 后端 OK 但前端代理 404 → Nginx `proxy_pass` 末尾要加 `/`

### ❌ 坑 13：Spring Cache @Cacheable 没生效
**排查**：启动类加 `@EnableCaching` + 方法 public + 外部调用 + 参数可序列化

### ❌ 坑 14：VectorStore.deleteAll() 方法不存在
**修复**：用 `vectorStore.delete(ids)` 代替，通过商品ID列表删除

### ❌ 坑 15：双数据源混淆（PgVectorStore 用了 MySQL 数据源）
**修复**：自定义 VectorStoreConfig + @Qualifier("pgVectorDataSource") 注入

### ❌ 坑 16：SpringDoc 2.5.0 与 Spring Boot 3.5 不兼容
**修复**：升级到 springdoc 2.8.9（兼容 Spring Framework 6.2）

### ❌ 坑 17：metadata cannot have null values
**修复**：存入 metadata 前对可能为 null 的字段做非空判断

### ❌ 坑 18：IDEA 启动后端端口被占用（Docker 后端也在跑）
**修复**：Docker 后端端口改为 8081，IDEA 本地用 8080，互不冲突

---

## 八、关键配置文件索引（快速查阅）

| 配置项 | 文件 | 行 |
|--------|------|----|
| 12 服务端口映射 + healthcheck | [docker-compose.yml](file:///D:/WebProject/wardrobe/docker-compose.yml) | L20-L260 |
| MySQL utf8mb4 启动参数 | [docker-compose.yml](file:///D:/WebProject/wardrobe/docker-compose.yml) | L28-L34 |
| 后端 Maven 阿里云镜像加速 | [wardrobe-backend/Dockerfile](file:///D:/WebProject/wardrobe/wardrobe-backend/Dockerfile) | L15-L40 |
| 向量数据库双数据源配置 | [DataSourceConfig.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/config/DataSourceConfig.java) | 全文 |
| PgVectorStore 自定义配置 | [VectorStoreConfig.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/config/VectorStoreConfig.java) | 全文 |
| RabbitMQ 队列交换机配置 | [RabbitMQConfig.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/config/RabbitMQConfig.java) | 全文 |
| API 文档配置 | [OpenApiConfig.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/config/OpenApiConfig.java) | 全文 |
| 密码加密配置 | [SecurityConfig.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/config/SecurityConfig.java) | 全文 |
| Sa-Token 权限配置 | [SaTokenConfig.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/config/SaTokenConfig.java) | 全文 |
| 向量搜索服务 | [ClothesEmbeddingService.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/service/ClothesEmbeddingService.java) | 全文 |
| AI 生成服务 | [AiGenerationService.java](file:///D:/WebProject/wardrobe/wardrobe-backend/src/main/java/com/itheima/service/AiGenerationService.java) | 全文 |
| 用户端 Vite + TS 配置 | [wardrobe-front-user/vite.config.ts](file:///D:/WebProject/wardrobe/wardrobe-front-user/vite.config.ts) | 全文 |
| 管理端 Nginx（含 rewrite）| [wardrobe-front-admin/nginx.conf](file:///D:/WebProject/wardrobe/wardrobe-front-admin/nginx.conf) | 全文 |
| Prometheus 采集配置 | [prometheus/prometheus.yml](file:///D:/WebProject/wardrobe/prometheus/prometheus.yml) | 全文 |

---

## 九、项目亮点（面试/简历加分项）

1. **AI 智能推荐**：基于 Spring AI + pgvector 的语义搜索和相似推荐，紧跟 2024-2026 技术热点
2. **双数据源架构**：MySQL 主库 + PostgreSQL 向量库，体现架构设计能力
3. **完整监控体系**：Prometheus + Grafana + Actuator，企业级可观测性方案
4. **消息队列异步处理**：RabbitMQ 订单异步解耦，体现高并发设计思维
5. **12 服务 Docker 编排**：完整 DevOps 能力，一键部署
6. **代码质量保障**：单元测试 + API 文档 + 密码加密，专业工程素养
7. **前后端分离双端**：用户端 + 管理端，TypeScript 全链路类型安全

---

## 十、交接完毕确认

- [ ] 接手人已启动 Docker Desktop，执行 `cd D:\WebProject\wardrobe && docker compose up -d`，10 分钟后 `docker compose ps` 全 healthy
- [ ] 接手人已导入 wardrobe.sql（按 3.3 节步骤，含 FLUSHALL Redis）
- [ ] 接手人能成功打开 http://localhost:7070/（用户端）并用 zhangsan/123123 登录
- [ ] 接手人能成功打开 http://localhost:7087/（管理端）并用 admin/admin123 登录
- [ ] 接手人能成功打开 http://localhost:8081/swagger-ui.html（API 文档）
- [ ] 接手人能完成完整端到端下单流程
- [ ] 接手人能在 IDEA 中本地启动后端（8080端口），与 Docker 后端（8081端口）互不冲突
- [ ] 接手人能运行单元测试：`mvn test` 全部通过
- [ ] 接手人能访问 Prometheus：http://localhost:9090 和 Grafana：http://localhost:3001
- [ ] 接手人能访问 RabbitMQ 管理后台：http://localhost:15672

> **交接人签字**：______________　　**日期**：__________
> **接手人签字**：______________　　**日期**：__________
