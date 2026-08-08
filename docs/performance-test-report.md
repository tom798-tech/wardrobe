# 云衣橱后端压测报告

## 1. 压测目标

本报告用于验证云衣橱后端在商品浏览类读请求下的稳定性，并为后续订单链路压测提供基线。当前 Codex 执行环境没有 `docker` 和 `k6` 命令，因此本次提交提供可复现脚本、指标口径和记录模板；需要在开发机或服务器上执行实测后填入结果。

## 2. 环境说明

| 项目 | 配置 |
| --- | --- |
| 后端入口 | `http://localhost:8081`，Docker Compose 后端映射端口 |
| 数据库 | MySQL 8，端口 `3307` |
| 缓存 | Redis 7，端口 `6380` |
| 消息队列 | RabbitMQ 3-management，端口 `5672` |
| 监控 | Prometheus `9090`，Grafana `3001` |
| 压测工具 | k6 |

## 3. 压测场景

| 场景 | 接口 | 目的 |
| --- | --- | --- |
| 健康检查 | `GET /actuator/health` | 验证基础可用性 |
| 商品列表 | `GET /clothes` | 核心读流量 |
| 商品详情 | `GET /clothes/1` | 主键查询 |
| 分类筛选 | `GET /clothes/type/1` | 验证 `idx_clothes_type` |
| 商品搜索 | `GET /clothes/search?keyword=T` | 验证模糊搜索压力 |

压测脚本位于 `scripts/load-test/wardrobe-smoke-load.js`。

## 4. 执行步骤

启动完整环境：

```powershell
cd D:\WebProject\wardrobe
docker compose up -d --build
```

确认后端和监控可用：

```powershell
curl http://localhost:8081/actuator/health
curl http://localhost:9090/-/ready
```

执行压测：

```powershell
cd D:\WebProject\wardrobe
k6 run .\scripts\load-test\wardrobe-smoke-load.js
```

压测其他地址：

```powershell
$env:BASE_URL="http://localhost:8080"
k6 run .\scripts\load-test\wardrobe-smoke-load.js
```

## 5. 通过标准

| 指标 | 目标 |
| --- | --- |
| 错误率 | `< 1%` |
| P95 响应时间 | `< 800ms` |
| 后端进程 CPU | 持续 `< 80%` |
| JVM 堆内存 | 持续 `< 85%` |
| 数据库连接池 | 活跃连接持续 `< 80%` |

## 6. 监控观察项

Prometheus/Grafana 中重点观察：

```promql
sum(rate(http_server_requests_seconds_count{job="wardrobe-backend"}[1m]))
sum(rate(http_server_requests_seconds_count{job="wardrobe-backend",status=~"5.."}[5m]))
sum(rate(http_server_requests_seconds_sum{job="wardrobe-backend"}[5m])) / sum(rate(http_server_requests_seconds_count{job="wardrobe-backend"}[5m]))
sum(jvm_memory_used_bytes{job="wardrobe-backend",area="heap"}) / sum(jvm_memory_max_bytes{job="wardrobe-backend",area="heap"})
process_cpu_usage{job="wardrobe-backend"}
hikaricp_connections_active{job="wardrobe-backend"} / hikaricp_connections_max{job="wardrobe-backend"}
```

## 7. 本次实测记录

| 项目 | 结果 |
| --- | --- |
| 是否执行 | 已执行 |
| 执行时间 | 2026-08-08 |
| 执行入口 | IDEA 本地后端 `http://localhost:8080` |
| 执行命令 | `k6 run .\scripts\load-test\wardrobe-smoke-load.js` |
| 输出文件 | `docs/k6-output-8080-after-cache-fix.txt` |
| 结果文件 | `docs/k6-summary-8080-after-cache-fix.json` |

| 并发阶段 | 请求数 | 错误率 | 平均响应时间 | P95 响应时间 | 最大响应时间 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| 10-30 VU ramping | 8830 | 0.00% | 3.49ms | 6.68ms | 34.5ms | 通过 |

## 8. 初步优化结论

- 商品分类、风格、订单、购物车、用户登录查重等高频查询已补充 BTree 索引。
- `LIKE '%keyword%'` 的商品搜索无法稳定命中普通 BTree 索引；数据量增大后应迁移到 FULLTEXT、Elasticsearch/OpenSearch，或复用 pgvector 语义检索。
- 压测时如果 P95 偏高，应先查看慢 SQL 日志和连接池告警，再判断是 SQL、连接池还是 JVM 资源瓶颈。
- 首次压测发现 `/clothes` 固定返回 500，原因是 Redis 中存在旧的 `clothes::all` 缓存值；删除该缓存后接口恢复正常，二次压测全部通过。后续改动实体或 Redis 序列化策略后，应主动清理相关缓存。
