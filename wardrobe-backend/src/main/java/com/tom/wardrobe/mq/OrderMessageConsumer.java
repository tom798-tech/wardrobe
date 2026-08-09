package com.tom.wardrobe.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.tom.wardrobe.config.RabbitMQConfig;
import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.entity.Order;
import com.tom.wardrobe.entity.OrderItem;
import com.tom.wardrobe.mapper.ClothesMapper;
import com.tom.wardrobe.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderMessageConsumer {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ClothesMapper clothesMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheManager cacheManager;

    private static final int MAX_RETRY_COUNT = 3;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderCreate(String orderIdStr, Message message, Channel channel) {
        Long orderId = null;
        try {
            orderId = Long.parseLong(orderIdStr);
            Integer retryCount = getRetryCount(message);
            log.info("收到订单创建消息，orderId: {}, retryCount: {}", orderId, retryCount);

            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                log.warn("订单不存在，确认消息，orderId: {}", orderId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            if (order.getStatus() != 0) {
                log.info("订单已处理，跳过消息，orderId: {}, status: {}", orderId, order.getStatus());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            List<OrderItem> orderItems = objectMapper.readValue(
                    order.getClothesDetails(),
                    new TypeReference<List<OrderItem>>() {}
            );
            log.info("订单详情解析完成，orderId: {}, itemCount: {}", orderId, orderItems.size());

            for (OrderItem item : orderItems) {
                Clothes clothes = clothesMapper.selectById(item.getClothId());
                if (clothes == null) {
                    log.warn("商品不存在，clothId: {}", item.getClothId());
                    continue;
                }

                int newStock = clothes.getStock() - item.getAmount();
                if (newStock < 0) {
                    log.error("数据库库存不足，orderId: {}, clothId: {}, stock: {}, amount: {}",
                            orderId, item.getClothId(), clothes.getStock(), item.getAmount());
                    order.setStatus(-1);
                    orderMapper.updateById(order);
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }

                clothes.setStock(newStock);
                clothesMapper.updateById(clothes);
                log.info("数据库库存扣减成功，clothId: {}, remainStock: {}", item.getClothId(), newStock);
            }
            evictClothesCache();

            order.setStatus(1);
            orderMapper.updateById(order);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("订单消息处理完成，orderId: {}", orderId);
        } catch (NumberFormatException e) {
            log.error("订单 ID 格式错误，orderIdStr: {}", orderIdStr, e);
            ackQuietly(message, channel);
        } catch (Exception e) {
            log.error("处理订单创建消息失败，orderId: {}", orderIdStr, e);
            handleRetry(message, channel, orderId);
        }
    }

    private void handleRetry(Message message, Channel channel, Long orderId) {
        Integer retryCount = getRetryCount(message);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("超过最大重试次数，消息进入死信队列，orderId: {}, retryCount: {}", orderId, retryCount);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            int nextRetryCount = retryCount + 1;
            republishWithRetryCount(message, channel, nextRetryCount);
            channel.basicAck(deliveryTag, false);
            log.info("订单消息已重新投递，orderId: {}, nextRetryCount: {}", orderId, nextRetryCount);
        } catch (Exception ex) {
            log.error("重新投递消息失败，保持原消息重新入队，orderId: {}", orderId, ex);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("消息重新入队失败，orderId: {}", orderId, ioException);
            }
        }
    }

    private void republishWithRetryCount(Message message, Channel channel, int retryCount) throws IOException {
        Map<String, Object> headers = new HashMap<>();
        if (message.getMessageProperties().getHeaders() != null) {
            headers.putAll(message.getMessageProperties().getHeaders());
        }
        headers.put("retry-count", retryCount);

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType(message.getMessageProperties().getContentType())
                .deliveryMode(2)
                .headers(headers)
                .build();

        channel.basicPublish(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                properties,
                message.getBody()
        );
    }

    private void ackQuietly(Message message, Channel channel) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("确认消息失败", ex);
        }
    }

    private Integer getRetryCount(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers == null) {
            return 0;
        }

        Object retryCountObj = headers.get("retry-count");
        if (retryCountObj instanceof Number) {
            return ((Number) retryCountObj).intValue();
        }
        if (retryCountObj instanceof String) {
            try {
                return Integer.parseInt((String) retryCountObj);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void evictClothesCache() {
        Cache cache = cacheManager.getCache("clothes");
        if (cache != null) {
            cache.clear();
        }
    }
}
