package com.tom.wardrobe.service;

import com.tom.wardrobe.config.RedisConfig;
import com.tom.wardrobe.util.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockService 单元测试
 * 测试 Redis Lua 脚本库存扣减的原子性和正确性
 */
@SpringBootTest
@Import(RedisConfig.class)
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private RedisUtils redisUtils;

    private static final Integer TEST_CLOTH_ID = 999;
    private static final int TEST_STOCK = 10;

    @BeforeEach
    void setUp() {
        // 初始化测试库存
        stockService.initStock(TEST_CLOTH_ID, TEST_STOCK);
    }

    @Test
    @DisplayName("测试初始化库存")
    void testInitStock() {
        int stock = stockService.getStock(TEST_CLOTH_ID);
        assertEquals(TEST_STOCK, stock);
    }

    @Test
    @DisplayName("测试获取库存")
    void testGetStock() {
        int stock = stockService.getStock(TEST_CLOTH_ID);
        assertEquals(TEST_STOCK, stock);

        // 测试不存在的商品
        int nonExistStock = stockService.getStock(9999);
        assertEquals(-1, nonExistStock);
    }

    @Test
    @DisplayName("测试扣减库存（成功）")
    void testDeductStockSuccess() {
        int deductAmount = 3;
        boolean result = stockService.deductStock(TEST_CLOTH_ID, deductAmount);

        assertTrue(result);
        assertEquals(TEST_STOCK - deductAmount, stockService.getStock(TEST_CLOTH_ID));
    }

    @Test
    @DisplayName("测试扣减库存（库存不足）")
    void testDeductStockInsufficient() {
        int deductAmount = TEST_STOCK + 1;
        boolean result = stockService.deductStock(TEST_CLOTH_ID, deductAmount);

        assertFalse(result);
        // 库存应该保持不变
        assertEquals(TEST_STOCK, stockService.getStock(TEST_CLOTH_ID));
    }

    @Test
    @DisplayName("测试扣减库存（扣减数量为0）")
    void testDeductStockZeroAmount() {
        boolean result = stockService.deductStock(TEST_CLOTH_ID, 0);

        assertFalse(result);
        assertEquals(TEST_STOCK, stockService.getStock(TEST_CLOTH_ID));
    }

    @Test
    @DisplayName("测试扣减库存（扣减数量为负数）")
    void testDeductStockNegativeAmount() {
        boolean result = stockService.deductStock(TEST_CLOTH_ID, -1);

        assertFalse(result);
        assertEquals(TEST_STOCK, stockService.getStock(TEST_CLOTH_ID));
    }

    @Test
    @DisplayName("测试回滚库存")
    void testRollbackStock() {
        // 先扣减
        stockService.deductStock(TEST_CLOTH_ID, 2);
        assertEquals(TEST_STOCK - 2, stockService.getStock(TEST_CLOTH_ID));

        // 回滚
        stockService.rollbackStock(TEST_CLOTH_ID, 2);
        assertEquals(TEST_STOCK, stockService.getStock(TEST_CLOTH_ID));
    }

    @Test
    @DisplayName("测试批量扣减库存（成功）")
    void testDeductStockBatchSuccess() {
        Integer[] clothIds = {1001, 1002, 1003};
        int[] amounts = {2, 3, 1};

        // 初始化库存
        stockService.initStock(1001, 5);
        stockService.initStock(1002, 5);
        stockService.initStock(1003, 5);

        boolean result = stockService.deductStockBatch(clothIds, amounts);

        assertTrue(result);
        assertEquals(3, stockService.getStock(1001));
        assertEquals(2, stockService.getStock(1002));
        assertEquals(4, stockService.getStock(1003));
    }

    @Test
    @DisplayName("测试批量扣减库存（库存不足）")
    void testDeductStockBatchInsufficient() {
        Integer[] clothIds = {2001, 2002};
        int[] amounts = {3, 3};

        // 初始化库存，第二个商品库存不足
        stockService.initStock(2001, 5);
        stockService.initStock(2002, 2);

        boolean result = stockService.deductStockBatch(clothIds, amounts);

        assertFalse(result);
        // 所有库存应该保持不变（原子性）
        assertEquals(5, stockService.getStock(2001));
        assertEquals(2, stockService.getStock(2002));
    }

    @Test
    @DisplayName("测试检查库存")
    void testCheckStock() {
        assertTrue(stockService.checkStock(TEST_CLOTH_ID, 5));
        assertTrue(stockService.checkStock(TEST_CLOTH_ID, TEST_STOCK));
        assertFalse(stockService.checkStock(TEST_CLOTH_ID, TEST_STOCK + 1));
    }

    @Test
    @DisplayName("测试批量检查库存")
    void testCheckStockBatch() {
        stockService.initStock(3001, 5);
        stockService.initStock(3002, 5);

        Integer[] clothIds = {3001, 3002};
        int[] amounts = {2, 3};

        assertTrue(stockService.checkStockBatch(clothIds, amounts));

        // 修改其中一个库存不足
        stockService.initStock(3001, 1);
        assertFalse(stockService.checkStockBatch(clothIds, amounts));
    }
}
