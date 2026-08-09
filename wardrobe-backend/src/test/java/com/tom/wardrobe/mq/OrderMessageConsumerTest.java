package com.tom.wardrobe.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.tom.wardrobe.config.RabbitMQConfig;
import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.entity.Order;
import com.tom.wardrobe.mapper.ClothesMapper;
import com.tom.wardrobe.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderMessageConsumerTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ClothesMapper clothesMapper;

    @Mock
    private Channel channel;

    private OrderMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderMessageConsumer();
        ReflectionTestUtils.setField(consumer, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(consumer, "clothesMapper", clothesMapper);
        ReflectionTestUtils.setField(consumer, "objectMapper", new ObjectMapper());
    }

    @Test
    void invalidOrderIdIsAckedAndNotRetried() throws Exception {
        Message message = message("not-number", 11L);

        consumer.handleOrderCreate("not-number", message, channel);

        verify(channel).basicAck(11L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
        verifyNoInteractions(orderMapper, clothesMapper);
    }

    @Test
    void failedMessageIsRepublishedWithIncrementedRetryCountAndOriginalAcked() throws Exception {
        Message message = message("100", 22L, Map.of("retry-count", 1L));
        when(orderMapper.selectById(100L)).thenThrow(new IllegalStateException("database unavailable"));

        consumer.handleOrderCreate("100", message, channel);

        ArgumentCaptor<AMQP.BasicProperties> propertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_ROUTING_KEY),
                propertiesCaptor.capture(),
                eq(message.getBody())
        );
        assertEquals(2, propertiesCaptor.getValue().getHeaders().get("retry-count"));
        verify(channel).basicAck(22L, false);
        verify(channel, never()).basicNack(eq(22L), anyBoolean(), anyBoolean());
    }

    @Test
    void failedMessageIsNackedToDeadLetterQueueAfterMaxRetries() throws Exception {
        Message message = message("100", 33L, Map.of("retry-count", "3"));
        when(orderMapper.selectById(100L)).thenThrow(new IllegalStateException("database unavailable"));

        consumer.handleOrderCreate("100", message, channel);

        verify(channel).basicNack(33L, false, false);
        verify(channel, never()).basicPublish(anyString(), anyString(), any(), any());
        verify(channel, never()).basicAck(eq(33L), anyBoolean());
    }

    @Test
    void republishFailureRequeuesOriginalMessage() throws Exception {
        Message message = message("100", 44L);
        when(orderMapper.selectById(100L)).thenThrow(new IllegalStateException("database unavailable"));
        doThrow(new java.io.IOException("rabbit publish failed"))
                .when(channel)
                .basicPublish(anyString(), anyString(), any(), any());

        consumer.handleOrderCreate("100", message, channel);

        verify(channel).basicNack(44L, false, true);
        verify(channel, never()).basicAck(eq(44L), anyBoolean());
    }

    @Test
    void successfulMessageDeductsStockUpdatesOrderAndAcks() throws Exception {
        Message message = message("100", 55L);
        Order order = order(100, 0, """
                [{"clothId":1,"amount":2}]
                """);
        Clothes clothes = new Clothes();
        clothes.setId(1);
        clothes.setStock(5);

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(clothesMapper.selectById(1)).thenReturn(clothes);

        consumer.handleOrderCreate("100", message, channel);

        assertEquals(3, clothes.getStock());
        assertEquals(1, order.getStatus());
        verify(clothesMapper).updateById(clothes);
        verify(orderMapper).updateById(order);
        verify(channel).basicAck(55L, false);
    }

    @Test
    void sameClothDifferentSizesAreCheckedAgainstTotalStock() throws Exception {
        Message message = message("100", 56L);
        Order order = order(100, 0, """
                [
                  {"clothId":1,"clothSize":"S","amount":6},
                  {"clothId":1,"clothSize":"M","amount":4}
                ]
                """);
        Clothes clothes = new Clothes();
        clothes.setId(1);
        clothes.setStock(9);

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(clothesMapper.selectById(1)).thenReturn(clothes);

        consumer.handleOrderCreate("100", message, channel);

        assertEquals(9, clothes.getStock());
        assertEquals(-1, order.getStatus());
        verify(clothesMapper, never()).updateById(any(Clothes.class));
        verify(orderMapper).updateById(order);
        verify(channel).basicAck(56L, false);
    }

    @Test
    void processedOrderIsAckedWithoutDuplicateStockDeduction() throws Exception {
        Message message = message("100", 66L);
        when(orderMapper.selectById(100L)).thenReturn(order(100, 1, "[]"));

        consumer.handleOrderCreate("100", message, channel);

        verify(channel).basicAck(66L, false);
        verifyNoInteractions(clothesMapper);
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    private Message message(String body, long deliveryTag) {
        return message(body, deliveryTag, Map.of());
    }

    private Message message(String body, long deliveryTag, Map<String, Object> headers) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        properties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        headers.forEach(properties::setHeader);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    private Order order(Integer id, Integer status, String clothesDetails) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setClothesDetails(clothesDetails);
        return order;
    }
}
