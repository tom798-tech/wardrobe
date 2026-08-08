package com.tom.wardrobe.config;

import com.tom.wardrobe.entity.Clothes;
import com.tom.wardrobe.mapper.ClothesMapper;
import com.tom.wardrobe.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 库存初始化配置
 * 在应用启动时将数据库库存同步到 Redis
 */
@Slf4j
@Component
public class StockInitConfig implements CommandLineRunner {

    @Resource
    private ClothesMapper clothesMapper;

    @Resource
    private RedisUtils redisUtils;

    /**
     * Redis 库存 Key 前缀
     */
    private static final String STOCK_KEY_PREFIX = "stock:";

    @Override
    public void run(String... args) {
        log.info("开始初始化 Redis 库存缓存...");

        try {
            List<Clothes> clothesList = clothesMapper.selectList(null);
            int count = 0;

            for (Clothes clothes : clothesList) {
                String key = STOCK_KEY_PREFIX + clothes.getId();
                redisUtils.set(key, clothes.getStock());
                count++;
            }

            log.info("Redis 库存缓存初始化完成，共 {} 件商品", count);
        } catch (Exception e) {
            log.error("初始化 Redis 库存缓存失败", e);
        }
    }
}
