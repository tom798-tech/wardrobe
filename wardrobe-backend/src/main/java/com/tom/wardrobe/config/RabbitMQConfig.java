package com.tom.wardrobe.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置
 * 
 * 功能：
 * 1. 订单队列（处理订单创建消息）
 * 2. 死信队列（处理消费失败的消息，支持延时重试）
 * 3. 消息确认机制（生产者确认、消费者手动确认）
 */
@Configuration
public class RabbitMQConfig {

    /**
     * ==================== 订单队列配置 ====================
     */
    public static final String ORDER_QUEUE = "wardrobe.order.queue";
    public static final String ORDER_EXCHANGE = "wardrobe.order.exchange";
    public static final String ORDER_ROUTING_KEY = "order.create";

    /**
     * ==================== 死信队列配置 ====================
     */
    public static final String ORDER_DLX_EXCHANGE = "wardrobe.order.dlx.exchange";
    public static final String ORDER_DLQ_QUEUE = "wardrobe.order.dlq.queue";
    public static final String ORDER_DLQ_ROUTING_KEY = "order.dlq";

    /**
     * 消息过期时间（毫秒）- 30分钟
     */
    private static final long MESSAGE_TTL = 30 * 60 * 1000L;

    /**
     * 创建死信交换机
     */
    @Bean
    public Exchange orderDlxExchange() {
        return ExchangeBuilder.directExchange(ORDER_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 创建死信队列
     */
    @Bean
    public Queue orderDlqQueue() {
        return QueueBuilder.durable(ORDER_DLQ_QUEUE)
                .build();
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding orderDlqBinding(Queue orderDlqQueue, Exchange orderDlxExchange) {
        return BindingBuilder.bind(orderDlqQueue)
                .to(orderDlxExchange)
                .with(ORDER_DLQ_ROUTING_KEY)
                .noargs();
    }

    /**
     * 创建订单队列（绑定死信队列）
     * 
     * 配置死信参数：
     * - x-dead-letter-exchange: 死信交换机
     * - x-dead-letter-routing-key: 死信路由键
     * - x-message-ttl: 消息过期时间
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", MESSAGE_TTL)
                .build();
    }

    /**
     * 创建订单交换机
     */
    @Bean
    public Exchange orderExchange() {
        return ExchangeBuilder.directExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 绑定订单队列到订单交换机
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, Exchange orderExchange) {
        return BindingBuilder.bind(orderQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY)
                .noargs();
    }

    /**
     * 配置 RabbitTemplate（生产者确认机制）
     * 
     * publisher-confirm-type: correlated - 发布确认模式
     * publisher-returns: true - 启用消息返回
     */
    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        
        // 启用发布确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                // 消息发送成功
                if (correlationData != null) {
                    System.out.println("消息发送成功，correlationId: " + correlationData.getId());
                }
            } else {
                // 消息发送失败，可进行重试或记录日志
                System.err.println("消息发送失败，cause: " + cause);
            }
        });
        
        // 启用消息返回（消息路由失败时返回）
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            System.err.println("消息返回，exchange: " + returnedMessage.getExchange() +
                    ", routingKey: " + returnedMessage.getRoutingKey() +
                    ", replyCode: " + returnedMessage.getReplyCode() +
                    ", replyText: " + returnedMessage.getReplyText());
        });
        
        return rabbitTemplate;
    }
}
