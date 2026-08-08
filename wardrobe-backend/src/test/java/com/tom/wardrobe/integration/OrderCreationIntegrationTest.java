package com.tom.wardrobe.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tom.wardrobe.entity.Order;
import com.tom.wardrobe.entity.OrderOutboxEvent;
import com.tom.wardrobe.mapper.OrderMapper;
import com.tom.wardrobe.mapper.OrderOutboxMapper;
import com.tom.wardrobe.mq.OrderMessageProducer;
import com.tom.wardrobe.service.OrderService;
import com.tom.wardrobe.service.StockService;
import com.tom.wardrobe.util.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "wardrobe.outbox.publish-fixed-delay=600000"
})
class OrderCreationIntegrationTest {

    private static final Integer TEST_USER_ID = 990001;
    private static final Integer TEST_CLOTH_ID = 990001;
    private static final String STOCK_KEY = "stock:" + TEST_CLOTH_ID;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockService stockService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderOutboxMapper orderOutboxMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private OrderMessageProducer orderMessageProducer;

    @BeforeEach
    void setUp() {
        cleanTestData();
        redisUtils.del(STOCK_KEY);
        reset(orderMessageProducer);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
        redisUtils.del(STOCK_KEY);
    }

    @Test
    void createOrderPersistsOrderDeductsRedisStockAndPublishesOutboxEvent() {
        stockService.initStock(TEST_CLOTH_ID, 5);

        String result = orderService.createOrder(testOrder(3));

        assertTrue(result.contains("订单创建成功"));

        List<Order> orders = orderMapper.findByUserId(TEST_USER_ID);
        assertEquals(1, orders.size());

        Order savedOrder = orders.get(0);
        assertEquals(0, savedOrder.getStatus());
        assertEquals(358.5, savedOrder.getPrice());
        assertNotNull(savedOrder.getTime());
        assertEquals(2, stockService.getStock(TEST_CLOTH_ID));

        OrderOutboxEvent outboxEvent = selectOutboxByOrderId(savedOrder.getId());
        assertNotNull(outboxEvent);
        assertEquals("ORDER", outboxEvent.getAggregateType());
        assertEquals("ORDER_CREATED", outboxEvent.getEventType());
        assertEquals(savedOrder.getId().toString(), outboxEvent.getPayload());
        assertEquals(OrderOutboxEvent.STATUS_SENT, outboxEvent.getStatus());
        assertEquals(0, outboxEvent.getRetryCount());

        verify(orderMessageProducer, timeout(1000)).sendOrderCreateMessage(savedOrder.getId().longValue());
    }

    @Test
    void createOrderDoesNotPersistWhenRedisStockIsInsufficient() {
        stockService.initStock(TEST_CLOTH_ID, 1);

        String result = orderService.createOrder(testOrder(2));

        assertTrue(result.contains("库存不足"));
        assertTrue(orderMapper.findByUserId(TEST_USER_ID).isEmpty());
        assertEquals(1, stockService.getStock(TEST_CLOTH_ID));
    }

    private Order testOrder(int amount) {
        Order order = new Order();
        order.setUserId(TEST_USER_ID);
        order.setPrice(119.5 * amount);
        order.setAddress("integration-test-address");
        order.setClothesDetails("""
                [{"clothId":990001,"clothName":"integration-test-cloth","amount":%d,"price":119.5,"clothSize":"M"}]
                """.formatted(amount));
        return order;
    }

    private OrderOutboxEvent selectOutboxByOrderId(Integer orderId) {
        return orderOutboxMapper.selectOne(new LambdaQueryWrapper<OrderOutboxEvent>()
                .eq(OrderOutboxEvent::getAggregateType, "ORDER")
                .eq(OrderOutboxEvent::getAggregateId, orderId.longValue()));
    }

    private void cleanTestData() {
        jdbcTemplate.update("""
                DELETE oo FROM t_order_outbox oo
                JOIN t_order o ON oo.aggregate_id = o.id
                WHERE o.user_id = ?
                """, TEST_USER_ID);
        jdbcTemplate.update("DELETE FROM t_order WHERE user_id = ?", TEST_USER_ID);
    }
}
