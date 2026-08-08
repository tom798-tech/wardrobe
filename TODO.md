# TODO

## 1. 权限控制风险
- [x] 修复 `ClothesController` 新增、修改、删除商品未限制管理员的问题。
- [x] 调整 `SaTokenConfig` 中 `/clothes/**`、`/brand/**`、`/type/**`、`/size/**` 的整段放行规则，避免写操作被未授权访问。
- [x] 补充公开读接口与后台写接口的权限边界说明或测试。

## 2. 订单一致性
- [x] 为订单创建流程补充事务边界。
- [x] 梳理订单入库、Redis 预扣库存、MQ 发送失败回滚之间的一致性策略。
- [x] 评估并实现本地事务 + 消息可靠投递 + 补偿任务或 Outbox 方案。

## 3. RabbitMQ 重试
- [x] 修正 `OrderMessageConsumer` 中 `retry-count` 读取后未递增的问题。
- [x] 确保失败消息按预期重试 3 次后进入死信队列。
- [x] 补充 MQ 消费失败与死信流转测试。

## 4. 配置安全
- [x] 将 `application.yml`、`application-docker.yml`、`docker-compose.yml` 中的硬编码密码和模拟 API key 改为环境变量或默认占位。
- [x] 区分本地开发配置与生产配置。
- [x] 在文档中说明生产环境应使用环境变量、配置中心或密钥管理。

## 5. 测试覆盖
- [x] 补充 Controller 权限测试。
- [x] 补充订单并发测试。
- [x] 补充 MQ 消费失败测试。
- [x] 补充集成测试。
- [x] 处理本地无法运行 `mvn test` 的环境问题。
- [x] 修复现有 `StockServiceTest` 的 Redis Lua 参数序列化问题。
- [x] 修复现有 `UserServiceTest` 的测试数据残留问题。

## 6. 可观测性与云原生
- [x] 补充链路追踪。
- [x] 补充结构化日志。
- [x] 补充告警规则。
- [x] 补充压测报告。
- [x] 补充慢 SQL 分析。

## 7. 后续工程化收口
- [x] 统一 `Clothes` 实体与 `t_clothes` 表结构，补齐 `brand_id`、`stock`、`description`、`sales` 并保证品牌筛选不再因缺列失败。
- [x] 补充 CI 流水线，自动拉起后端依赖并执行 Maven 测试。
- [x] 升级商品搜索能力，避免 `/clothes/search` 只依赖 `LIKE '%keyword%'`。
