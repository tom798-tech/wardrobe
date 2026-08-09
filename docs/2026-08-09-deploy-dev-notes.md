# 2026-08-09 部署与开发问题复盘

本文记录今天早上部署和联调过程中暴露出的关键问题、根因和处理方式，便于后续写 README、答辩总结或继续完善项目。

## 1. GitHub Actions 持续失败

**现象**

GitHub 持续发送邮件，提示 `Backend CI` 的 `Maven tests` 失败。

**根因**

订单消费者 `OrderMessageConsumer` 新增了清理 `clothes` 缓存逻辑，但单元测试是手动 `new OrderMessageConsumer()`，只注入了 `orderMapper`、`clothesMapper`、`objectMapper`，没有注入 `cacheManager`。测试执行到清缓存时出现空指针，导致后续 `order.setStatus(1)` 没有执行。

**处理**

在 `evictClothesCache()` 中增加空值保护：

```java
if (cacheManager == null) {
    return;
}
```

**沉淀**

单元测试中手动构造对象时，要注意新增依赖是否也被注入。缓存、消息、监控这类横切依赖应允许在单元测试里缺省，否则容易破坏原有测试。

## 2. 管理端与用户端同时打开后，管理端出现 403

**现象**

管理端首页、订单统计或用户管理接口报：

```text
Request failed with status code 403
```

**根因**

后端 Sa-Token 配置的请求头名称是：

```yaml
token-name: token
```

用户端 axios 使用 `token` 请求头，但管理端 axios 只发送：

```http
Authorization: Bearer xxx
```

后端无法按配置读取管理员 token，访问 `/order/all`、`/user` 等管理员接口时被判定为无权限。

**处理**

管理端 axios 同时发送：

```ts
config.headers.token = u.token
config.headers.Authorization = `Bearer ${u.token}`
```

**沉淀**

多端项目必须统一认证协议。前端用户端、管理端和后端安全配置应明确约定 token header 名称，避免一个端可用、另一个端权限异常。

## 3. 管理端订单数、待发货、最近订单与用户端不一致

**现象**

用户端已有订单，但管理端首页显示：

```text
订单数 0
待发货 0
最近订单 暂无数据
```

**根因**

后端 `/order` 接口已经改成“当前登录用户订单”，用于修复水平越权。管理端首页和订单管理仍然调用 `/order`，于是拿到的是管理员账号自己的订单，而不是全站订单。

**处理**

管理端改用管理员接口：

```ts
request.get('/order/all')
```

同时将管理端订单状态文案统一为：

```text
待支付 / 待发货 / 待收货 / 已完成
```

**沉淀**

用户端和管理端不能混用同一个列表接口。用户端应使用“当前用户视角”，管理端应使用“全局管理视角”。

## 4. 商品库存扣减后，首页仍显示旧库存

**现象**

用户购买了库存为 119 的商品后，订单状态已经变为待发货，但首页商品库存仍显示 119。

**根因**

订单流程中数据库库存已由 RabbitMQ 消费者扣减，但首页 `/clothes` 使用了 Spring Cache：

```java
@Cacheable(key = "'all'")
public List<Clothes> findAll()
```

订单扣库存直接调用 `clothesMapper.updateById(clothes)`，没有清理 `clothes` 缓存，导致首页继续读旧缓存。

**处理**

订单消费者扣减数据库库存成功后，清理商品缓存：

```java
Cache cache = cacheManager.getCache("clothes");
if (cache != null) {
    cache.clear();
}
```

已存在的旧缓存可手动清理：

```bash
sudo docker exec wardrobe-redis sh -c 'redis-cli --scan --pattern "clothes::*" | xargs -r redis-cli DEL'
```

**沉淀**

只要有缓存，就要明确缓存失效时机。库存属于高变化数据，扣减、补库存、商品编辑都应同步处理缓存和 Redis 库存。

## 5. 管理端缺少补库存功能

**现象**

商品卖完后，管理端没有入口修改库存，导致无法补货。

**根因**

管理端上架和编辑服装表单没有 `stock` 字段；后端更新服装后也没有同步 Redis 中的 `stock:{clothId}`。

**处理**

管理端新增：

- 服装管理列表展示库存
- 上架服装填写初始库存
- 编辑服装可修改库存

后端新增：

- 上架服装时初始化 Redis 库存
- 修改库存后同步 Redis 库存
- 商品缓存继续通过 `@CacheEvict` 清理

**沉淀**

库存管理不应只改 MySQL。项目中下单依赖 Redis 预扣库存，因此补库存必须同时同步数据库和 Redis。

## 6. 管理端图片不显示

**现象**

管理端服装管理列表图片显示“加载失败”。

**根因**

前端 Nginx 中 `/api/images/*.jpg` 本应代理到后端，但静态资源正则规则可能抢先匹配图片请求，导致请求没有转发给后端。

**处理**

管理端和用户端 Nginx 配置将 `/api/` 改为高优先级匹配：

```nginx
location ^~ /api/ {
    proxy_pass http://wardrobe-backend:8080/;
}
```

如果后端容器重建后前端仍 502，需要重启前端容器，让 Nginx 重新解析后端服务名：

```bash
sudo docker compose restart wardrobe-front-user wardrobe-front-admin
```

**沉淀**

前后端分离部署时，图片、上传文件、API 都可能经过 Nginx 代理。静态资源规则和 API 代理规则的优先级要特别注意。

## 7. 管理员登录失败

**现象**

使用：

```text
admin / Admin@2026
```

管理端提示用户名或密码错误。

**根因**

初始 `wardrobe.sql` 中 admin 密码可能仍是明文 `admin`，但后端登录已经使用 BCrypt 校验。数据库里的密码必须是 BCrypt hash。

**处理**

通过 SQL 强制重置 admin 密码为 `Admin@2026` 对应的 BCrypt hash，并确认 `role = 1`。

**沉淀**

部署数据库初始化脚本时，要保证密码存储格式与后端认证逻辑一致。不能一边使用 BCrypt，一边导入明文密码。

## 8. 商品详情页提示无权限

**现象**

用户点击衣服详情页时，提示：

```text
没有权限访问此资源
```

**根因**

详情页加载评论后，又请求 `/user` 管理接口来补评论用户名。普通用户和游客没有管理员权限，所以被后端拒绝。

**处理**

详情页移除 `/user` 请求，直接使用评论接口返回的 `userName`，没有则显示“匿名用户”。

**沉淀**

用户端页面不能调用管理端接口。评论列表这类公开展示数据，应由评论接口直接返回必要的展示字段。

## 9. 订单提交幂等性问题

**现象**

购物车提交订单时先后出现：

```text
缺少幂等性 Token，请先获取！
请勿重复提交
```

**根因**

第一阶段：前端提交订单没有先获取幂等 token，也没有带 `X-Idempotent-Token` 请求头。

第二阶段：幂等 token 存入 Redis 时使用 JSON 序列化，而 Lua 脚本比较 Redis 原始值和 Java 反序列化值，导致第一次提交也被误判为重复提交。

**处理**

前端：

- 提交订单前获取幂等 token
- 请求头携带 `X-Idempotent-Token`

后端：

- 对随机 token 使用“存在则原子删除”的 Lua 逻辑
- token 不存在才判定过期或重复提交

**沉淀**

幂等设计要同时覆盖前端调用流程和后端原子校验。Redis 序列化方式会影响 Lua 脚本比较，不能只看 Java 侧反序列化结果。

## 10. 订单商品图片缺失

**现象**

用户端“我的订单”中商品图片显示 `No Image`。

**根因**

订单详情 `clothesDetails` 中只保存了商品名、尺码、价格、数量，没有保存图片字段。订单页只读 `cover` 字段，因此无法显示图片。

**处理**

新增订单时保存：

```text
cover / image / images
```

订单页兼容旧订单：如果订单详情没有图片，则根据 `clothId` 从商品列表中补图。

**沉淀**

订单快照应保存下单当时必要的商品展示信息，包括名称、价格、尺码、图片。否则商品后续变更或接口缺字段时，历史订单展示会不完整。

## 总结

今天暴露的问题集中在四类：

1. **部署一致性**：数据库初始化、Docker 容器重建、Nginx 代理规则需要与代码版本一致。
2. **权限边界**：用户端不能调用管理端接口，管理端必须调用管理员视角接口。
3. **缓存一致性**：库存变更必须同步 MySQL、Redis 库存和商品列表缓存。
4. **自动化验证**：CI 能及时发现单元测试被新依赖破坏，是项目工程化亮点。

这些问题处理后，项目比单纯“能跑”更进一步，开始具备部署、权限、缓存、幂等和 CI 的工程化闭环。
