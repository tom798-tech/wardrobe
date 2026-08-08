package com.tom.wardrobe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tom.wardrobe.entity.Order;
import com.tom.wardrobe.entity.OrderOutboxEvent;
import com.tom.wardrobe.mapper.OrderMapper;
import com.tom.wardrobe.mapper.UserMapper;
import com.tom.wardrobe.util.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceConcurrencyTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderOutboxService orderOutboxService;

    @Mock
    private StockService stockService;

    @Mock
    private RedisDistributedLock distributedLock;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        ReflectionTestUtils.setField(orderService, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(orderService, "userMapper", userMapper);
        ReflectionTestUtils.setField(orderService, "orderOutboxService", orderOutboxService);
        ReflectionTestUtils.setField(orderService, "stockService", stockService);
        ReflectionTestUtils.setField(orderService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(orderService, "distributedLock", distributedLock);
    }

    @Test
    void concurrentCreateOrderForSameUserOnlyAllowsOneRequestIntoOrderPipeline() throws Exception {
        int threadCount = 20;
        AtomicBoolean lockGranted = new AtomicBoolean(false);
        OrderOutboxEvent outboxEvent = new OrderOutboxEvent();
        outboxEvent.setId(1L);

        when(distributedLock.tryLock(eq("order:user:7"), anyLong()))
                .thenAnswer(invocation -> lockGranted.compareAndSet(false, true) ? "lock-value" : null);
        when(stockService.deductStockBatch(any(Integer[].class), any(int[].class))).thenReturn(true);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100);
            return 1;
        });
        when(orderOutboxService.createOrderCreatedEvent(100)).thenReturn(outboxEvent);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orderService.createOrder(orderForUser(7));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdownNow();

        verify(stockService, times(1)).deductStockBatch(any(Integer[].class), any(int[].class));
        verify(orderMapper, times(1)).insert(any(Order.class));
        verify(orderOutboxService, times(1)).createOrderCreatedEvent(100);
        verify(orderOutboxService, times(1)).publishAfterCommit(1L);
        verify(distributedLock, times(1)).unlock("order:user:7", "lock-value");
    }

    private Order orderForUser(Integer userId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setPrice(99.0);
        order.setAddress("test-address");
        order.setClothesDetails("""
                [{"clothId":1,"amount":1,"price":99.0}]
                """);
        return order;
    }
}
