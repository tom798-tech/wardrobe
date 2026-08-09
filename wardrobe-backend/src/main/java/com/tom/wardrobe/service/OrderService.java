package com.tom.wardrobe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tom.wardrobe.entity.Order;
import com.tom.wardrobe.entity.OrderItem;
import com.tom.wardrobe.entity.User;
import com.tom.wardrobe.mapper.OrderMapper;
import com.tom.wardrobe.mapper.UserMapper;
import com.tom.wardrobe.util.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderService {

    private static final long ORDER_LOCK_EXPIRE_TIME = 10 * 1000L;
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private OrderOutboxService orderOutboxService;

    @Resource
    private StockService stockService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RedisDistributedLock distributedLock;

    public List<Order> findByUserId(Integer userId) {
        return orderMapper.findByUserId(userId);
    }

    public List<Order> findAll() {
        List<Order> orders = orderMapper.selectList(null);
        fillUserInfo(orders);
        return orders;
    }

    public List<Order> findByStatus(Integer status) {
        return orderMapper.findByStatus(status);
    }

    public Order findById(Integer id) {
        Order order = orderMapper.selectById(id);
        fillUserInfo(order);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Order order) {
        Integer userId = order.getUserId();
        String lockKey = "order:user:" + userId;
        String lockValue = null;
        List<OrderItem> orderItems = null;
        boolean stockDeducted = false;

        try {
            lockValue = distributedLock.tryLock(lockKey, ORDER_LOCK_EXPIRE_TIME);
            if (lockValue == null) {
                log.warn("用户正在下单中，拒绝重复请求，userId: {}", userId);
                return "请不要重复下单！";
            }

            orderItems = parseOrderItems(order);
            Map<Integer, Integer> stockAmounts = aggregateStockAmounts(orderItems);
            Integer[] clothIds = stockAmounts.keySet().toArray(new Integer[0]);
            int[] amounts = stockAmounts.values().stream().mapToInt(Integer::intValue).toArray();

            stockDeducted = stockService.deductStockBatch(clothIds, amounts);
            if (!stockDeducted) {
                log.warn("库存预扣失败，订单创建取消，userId: {}", userId);
                return "库存不足！";
            }

            order.setTime(LocalDateTime.now().format(ORDER_TIME_FORMATTER));
            order.setStatus(0);
            int count = orderMapper.insert(order);
            if (count <= 0) {
                rollbackStock(orderItems);
                stockDeducted = false;
                return "订单创建失败！";
            }

            Long outboxEventId = orderOutboxService.createOrderCreatedEvent(order.getId()).getId();
            orderOutboxService.publishAfterCommit(outboxEventId);

            return "订单创建成功！";
        } catch (IllegalArgumentException e) {
            log.error("订单参数错误", e);
            return e.getMessage();
        } catch (RuntimeException e) {
            if (stockDeducted && orderItems != null) {
                rollbackStock(orderItems);
            }
            throw e;
        } finally {
            if (lockValue != null) {
                distributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    public String updateOrder(Order order) {
        int count = orderMapper.updateById(order);
        return count > 0 ? "订单更新成功！" : "订单更新失败！";
    }

    public String deleteOrder(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order != null && order.getStatus() == 0) {
            try {
                rollbackStock(parseOrderItems(order));
            } catch (Exception e) {
                log.error("回滚库存失败，orderId: {}", id, e);
            }
        }

        int count = orderMapper.deleteById(id);
        return count > 0 ? "订单删除成功！" : "订单删除失败！";
    }

    public String payOrder(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            order.setStatus(1);
            orderMapper.updateById(order);
            return "支付成功！";
        }
        return "订单不存在！";
    }

    public String receiveOrder(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            order.setStatus(3);
            orderMapper.updateById(order);
            return "收货成功！";
        }
        return "订单不存在！";
    }

    public String deliveryOrder(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            order.setStatus(2);
            orderMapper.updateById(order);
            return "发货成功！";
        }
        return "订单不存在！";
    }

    private void fillUserInfo(List<Order> orders) {
        for (Order order : orders) {
            fillUserInfo(order);
        }
    }

    private void fillUserInfo(Order order) {
        if (order != null && order.getUserId() != null) {
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                order.setUserName(user.getUserName());
                order.setPhone(user.getPhone());
            }
        }
    }

    private List<OrderItem> parseOrderItems(Order order) {
        try {
            List<OrderItem> orderItems = objectMapper.readValue(
                    order.getClothesDetails(),
                    new TypeReference<List<OrderItem>>() {}
            );
            if (orderItems == null || orderItems.isEmpty()) {
                throw new IllegalArgumentException("订单商品不能为空！");
            }
            return orderItems;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("订单详情格式错误！");
        }
    }

    private void rollbackStock(List<OrderItem> orderItems) {
        aggregateStockAmounts(orderItems).forEach(stockService::rollbackStock);
    }

    private Map<Integer, Integer> aggregateStockAmounts(List<OrderItem> orderItems) {
        Map<Integer, Integer> amounts = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            if (item.getClothId() == null || item.getAmount() == null || item.getAmount() <= 0) {
                throw new IllegalArgumentException("订单商品信息不完整！");
            }
            amounts.merge(item.getClothId(), item.getAmount(), Integer::sum);
        }
        return amounts;
    }
}
