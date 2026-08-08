package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.OrderOutboxEvent;
import com.tom.wardrobe.mapper.OrderOutboxMapper;
import com.tom.wardrobe.mq.OrderMessageProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderOutboxServiceTest {

    @Mock
    private OrderOutboxMapper orderOutboxMapper;

    @Mock
    private OrderMessageProducer orderMessageProducer;

    @Test
    void publishByIdMarksEventSentWhenMessageSendSucceeds() {
        OrderOutboxEvent event = pendingEvent();
        when(orderOutboxMapper.selectById(1L)).thenReturn(event);

        OrderOutboxService service = new OrderOutboxService(orderOutboxMapper, orderMessageProducer);
        service.publishById(1L);

        verify(orderMessageProducer).sendOrderCreateMessage(100L);
        ArgumentCaptor<OrderOutboxEvent> captor = ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(orderOutboxMapper).updateById(captor.capture());
        assertEquals(OrderOutboxEvent.STATUS_SENT, captor.getValue().getStatus());
        assertNull(captor.getValue().getLastError());
    }

    @Test
    void publishByIdKeepsEventForRetryWhenMessageSendFails() {
        OrderOutboxEvent event = pendingEvent();
        when(orderOutboxMapper.selectById(1L)).thenReturn(event);
        doThrow(new IllegalStateException("rabbit unavailable"))
                .when(orderMessageProducer).sendOrderCreateMessage(100L);

        OrderOutboxService service = new OrderOutboxService(orderOutboxMapper, orderMessageProducer);
        service.publishById(1L);

        ArgumentCaptor<OrderOutboxEvent> captor = ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(orderOutboxMapper).updateById(captor.capture());
        OrderOutboxEvent updated = captor.getValue();
        assertEquals(OrderOutboxEvent.STATUS_FAILED, updated.getStatus());
        assertEquals(1, updated.getRetryCount());
        assertTrue(updated.getNextRetryTime().isAfter(LocalDateTime.now()));
        assertTrue(updated.getLastError().contains("rabbit unavailable"));
    }

    private OrderOutboxEvent pendingEvent() {
        OrderOutboxEvent event = new OrderOutboxEvent();
        event.setId(1L);
        event.setAggregateType("ORDER");
        event.setAggregateId(100L);
        event.setEventType("ORDER_CREATED");
        event.setPayload("100");
        event.setStatus(OrderOutboxEvent.STATUS_PENDING);
        event.setRetryCount(0);
        event.setNextRetryTime(LocalDateTime.now());
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }
}
