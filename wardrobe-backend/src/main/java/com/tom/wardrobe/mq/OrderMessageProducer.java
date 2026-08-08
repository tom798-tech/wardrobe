package com.tom.wardrobe.mq;

import com.tom.wardrobe.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 订单消息生产者
 * 用于发送订单创建消息到 RabbitMQ
 */
@Slf4j
@Component
public class OrderMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送订单创建消息
     *
     * @param orderId 订单ID
     */
    public void sendOrderCreateMessage(Long orderId) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_ROUTING_KEY,
                    orderId.toString()
            );
            log.info("订单创建消息已发送，orderId: {}", orderId);
        } catch (Exception e) {
            log.error("发送订单创建消息失败，orderId: {}", orderId, e);
            throw e;
        }
    }
}