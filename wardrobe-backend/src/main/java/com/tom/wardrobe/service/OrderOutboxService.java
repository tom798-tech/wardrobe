package com.tom.wardrobe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tom.wardrobe.entity.OrderOutboxEvent;
import com.tom.wardrobe.mapper.OrderOutboxMapper;
import com.tom.wardrobe.mq.OrderMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderOutboxService {

    private static final int BATCH_SIZE = 20;
    private static final int MAX_ERROR_LENGTH = 512;
    private static final String AGGREGATE_TYPE_ORDER = "ORDER";
    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";

    private final OrderOutboxMapper orderOutboxMapper;
    private final OrderMessageProducer orderMessageProducer;

    public OrderOutboxService(OrderOutboxMapper orderOutboxMapper, OrderMessageProducer orderMessageProducer) {
        this.orderOutboxMapper = orderOutboxMapper;
        this.orderMessageProducer = orderMessageProducer;
    }

    public OrderOutboxEvent createOrderCreatedEvent(Integer orderId) {
        LocalDateTime now = LocalDateTime.now();
        OrderOutboxEvent event = new OrderOutboxEvent();
        event.setAggregateType(AGGREGATE_TYPE_ORDER);
        event.setAggregateId(orderId.longValue());
        event.setEventType(EVENT_TYPE_ORDER_CREATED);
        event.setPayload(orderId.toString());
        event.setStatus(OrderOutboxEvent.STATUS_PENDING);
        event.setRetryCount(0);
        event.setNextRetryTime(now);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        orderOutboxMapper.insert(event);
        return event;
    }

    public void publishAfterCommit(Long eventId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishById(eventId);
                }
            });
            return;
        }
        publishById(eventId);
    }

    @Scheduled(fixedDelayString = "${wardrobe.outbox.publish-fixed-delay:10000}")
    public void publishPending() {
        List<OrderOutboxEvent> events = orderOutboxMapper.selectList(
                new LambdaQueryWrapper<OrderOutboxEvent>()
                        .in(OrderOutboxEvent::getStatus,
                                OrderOutboxEvent.STATUS_PENDING,
                                OrderOutboxEvent.STATUS_FAILED)
                        .le(OrderOutboxEvent::getNextRetryTime, LocalDateTime.now())
                        .orderByAsc(OrderOutboxEvent::getCreatedAt)
                        .last("LIMIT " + BATCH_SIZE)
        );

        for (OrderOutboxEvent event : events) {
            publish(event);
        }
    }

    public void publishById(Long eventId) {
        OrderOutboxEvent event = orderOutboxMapper.selectById(eventId);
        if (event == null) {
            log.warn("Outbox 事件不存在，eventId: {}", eventId);
            return;
        }
        publish(event);
    }

    private void publish(OrderOutboxEvent event) {
        if (OrderOutboxEvent.STATUS_SENT == event.getStatus()) {
            return;
        }

        try {
            orderMessageProducer.sendOrderCreateMessage(Long.valueOf(event.getPayload()));
            markSent(event);
            log.info("Outbox 事件投递成功，eventId: {}, orderId: {}", event.getId(), event.getPayload());
        } catch (Exception e) {
            markFailed(event, e);
            log.error("Outbox 事件投递失败，eventId: {}, orderId: {}", event.getId(), event.getPayload(), e);
        }
    }

    private void markSent(OrderOutboxEvent event) {
        event.setStatus(OrderOutboxEvent.STATUS_SENT);
        event.setLastError(null);
        event.setUpdatedAt(LocalDateTime.now());
        orderOutboxMapper.updateById(event);
    }

    private void markFailed(OrderOutboxEvent event, Exception e) {
        int retryCount = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
        event.setStatus(OrderOutboxEvent.STATUS_FAILED);
        event.setRetryCount(retryCount);
        event.setNextRetryTime(LocalDateTime.now().plusSeconds(nextRetryDelaySeconds(retryCount)));
        event.setLastError(truncateError(e));
        event.setUpdatedAt(LocalDateTime.now());
        orderOutboxMapper.updateById(event);
    }

    private long nextRetryDelaySeconds(int retryCount) {
        return Math.min(300, 10L * retryCount);
    }

    private String truncateError(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
