package com.tom.wardrobe.service;

import com.tom.wardrobe.config.RedisConfig;
import com.tom.wardrobe.util.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(RedisConfig.class)
class StockConcurrencyTest {

    private static final Integer CONCURRENT_CLOTH_ID = 880001;
    private static final String STOCK_KEY = "stock:" + CONCURRENT_CLOTH_ID;

    @Autowired
    private StockService stockService;

    @Autowired
    private RedisUtils redisUtils;

    @AfterEach
    void tearDown() {
        redisUtils.del(STOCK_KEY);
    }

    @Test
    void concurrentDeductStockDoesNotOversell() throws Exception {
        int initialStock = 10;
        int threadCount = 40;
        stockService.initStock(CONCURRENT_CLOTH_ID, initialStock);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> errors = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (stockService.deductStock(CONCURRENT_CLOTH_ID, 1)) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertTrue(errors.isEmpty(), () -> "并发扣库存出现异常: " + errors);
        assertEquals(initialStock, successCount.get());
        assertEquals(0, stockService.getStock(CONCURRENT_CLOTH_ID));
    }
}
