package com.tom.wardrobe.service;

import com.tom.wardrobe.mapper.ClothesMapper;
import com.tom.wardrobe.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 库存服务 - 基于 Redis Lua 脚本实现原子扣减库存
 * 
 * 解决库存超卖问题的核心方案：
 * 1. Redis 预扣库存（下单时校验并预扣）
 * 2. Lua 脚本保证原子性
 * 3. 数据库最终一致性（MQ异步同步）
 */
@Slf4j
@Service
public class StockService {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ClothesMapper clothesMapper;

    /**
     * Redis 库存 Key 前缀
     */
    private static final String STOCK_KEY_PREFIX = "stock:";

    /**
     * Lua 脚本：原子扣减库存
     * 
     * 逻辑：
     * 1. 获取当前库存
     * 2. 如果库存 >= 扣减数量，扣减并返回 1（成功）
     * 3. 如果库存 < 扣减数量，返回 0（失败）
     */
    private static final String DECREMENT_STOCK_LUA = 
            "local stock = tonumber(redis.call('get', KEYS[1]))\n" +
            "if stock and stock >= tonumber(ARGV[1]) then\n" +
            "    redis.call('decrby', KEYS[1], ARGV[1])\n" +
            "    return 1\n" +
            "end\n" +
            "return 0";

    /**
     * Lua 脚本：原子扣减多个商品库存
     * 
     * KEYS: 库存Key列表 [stock:1, stock:2, ...]
     * ARGV: 扣减数量列表 [1, 2, ...]
     * 
     * 返回值：
     * -1: 某个商品库存不足
     *  0: 所有商品扣减成功
     */
    private static final String DECREMENT_MULTI_STOCK_LUA =
            "for i = 1, #KEYS do\n" +
            "    local stock = tonumber(redis.call('get', KEYS[i]))\n" +
            "    if not stock or stock < tonumber(ARGV[i]) then\n" +
            "        return -1\n" +
            "    end\n" +
            "end\n" +
            "for i = 1, #KEYS do\n" +
            "    redis.call('decrby', KEYS[i], ARGV[i])\n" +
            "end\n" +
            "return 0";

    /**
     * 获取商品库存 Key
     */
    private String getStockKey(Integer clothId) {
        return STOCK_KEY_PREFIX + clothId;
    }

    /**
     * 初始化商品库存到 Redis
     * 
     * @param clothId 商品ID
     * @param stock   库存数量
     * @return 是否成功
     */
    public boolean initStock(Integer clothId, int stock) {
        String key = getStockKey(clothId);
        return redisUtils.set(key, stock);
    }

    /**
     * 获取当前库存（从 Redis）
     * 
     * @param clothId 商品ID
     * @return 库存数量，-1表示库存Key不存在
     */
    public int getStock(Integer clothId) {
        String key = getStockKey(clothId);
        Object value = redisUtils.get(key);
        if (value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 预扣库存（原子操作）
     * 
     * @param clothId 商品ID
     * @param amount  扣减数量
     * @return true-扣减成功，false-库存不足或失败
     */
    public boolean deductStock(Integer clothId, int amount) {
        if (amount <= 0) {
            return false;
        }

        String key = getStockKey(clothId);
        Object result = redisUtils.executeLua(
                DECREMENT_STOCK_LUA,
                new String[]{key},
                new String[]{String.valueOf(amount)}
        );

        return result != null && "1".equals(result.toString());
    }

    /**
     * 批量预扣库存（原子操作）
     * 
     * @param clothIds 商品ID数组
     * @param amounts  扣减数量数组
     * @return true-全部扣减成功，false-任意商品库存不足或失败
     */
    public boolean deductStockBatch(Integer[] clothIds, int[] amounts) {
        if (clothIds == null || amounts == null || clothIds.length != amounts.length) {
            return false;
        }

        String[] keys = new String[clothIds.length];
        String[] args = new String[amounts.length];

        for (int i = 0; i < clothIds.length; i++) {
            keys[i] = getStockKey(clothIds[i]);
            args[i] = String.valueOf(amounts[i]);
        }

        Object result = redisUtils.executeLua(
                DECREMENT_MULTI_STOCK_LUA,
                keys,
                args
        );

        return result != null && "0".equals(result.toString());
    }

    /**
     * 回滚库存（取消订单时使用）
     * 
     * @param clothId 商品ID
     * @param amount  回滚数量
     * @return 是否成功
     */
    public boolean rollbackStock(Integer clothId, int amount) {
        if (amount <= 0) {
            return false;
        }

        String key = getStockKey(clothId);
        redisUtils.incr(key, amount);
        return true;
    }

    /**
     * 从数据库同步库存到 Redis（启动时初始化或手动刷新）
     * 
     * @param clothId 商品ID
     * @return 是否成功
     */
    public boolean syncStockFromDb(Integer clothId) {
        com.tom.wardrobe.entity.Clothes clothes = clothesMapper.selectById(clothId);
        if (clothes == null) {
            return false;
        }
        return initStock(clothId, clothes.getStock());
    }

    /**
     * 检查库存是否充足（不扣减，仅检查）
     * 
     * @param clothId 商品ID
     * @param amount  需要的数量
     * @return 是否充足
     */
    public boolean checkStock(Integer clothId, int amount) {
        int stock = getStock(clothId);
        return stock >= amount;
    }

    /**
     * 批量检查库存（不扣减，仅检查）
     * 
     * @param clothIds 商品ID数组
     * @param amounts  需要的数量数组
     * @return 是否全部充足
     */
    public boolean checkStockBatch(Integer[] clothIds, int[] amounts) {
        if (clothIds == null || amounts == null || clothIds.length != amounts.length) {
            return false;
        }

        for (int i = 0; i < clothIds.length; i++) {
            if (!checkStock(clothIds[i], amounts[i])) {
                return false;
            }
        }
        return true;
    }
}
