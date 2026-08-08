package com.tom.wardrobe.mq;

import com.tom.wardrobe.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 * 
 * 当消息被拒绝（reject）或过期后，会进入死信队列
 * 这里可以记录日志、告警、或人工处理
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    /**
     * 处理死信消息
     * 
     * 死信消息来源：
     * 1. 消费者抛出异常，消息被重新投递超过最大重试次数
     * 2. 消息在队列中过期（TTL）
     * 3. 队列达到最大长度，消息被丢弃
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_DLQ_QUEUE)
    public void handleDeadLetter(String message) {
        try {
            log.error("收到死信消息，orderId: {}, 请人工处理或排查问题", message);
            
            // 可以在这里实现：
            // 1. 记录到数据库或日志系统
            // 2. 发送告警通知（邮件、短信、钉钉等）
            // 3. 尝试重新投递（需要防止无限循环）
            
        } catch (Exception e) {
            log.error("处理死信消息异常，message: {}", message, e);
        }
    }
}
