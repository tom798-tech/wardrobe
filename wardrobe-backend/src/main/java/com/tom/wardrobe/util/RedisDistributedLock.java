package com.tom.wardrobe.util;

import com.tom.wardrobe.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.UUID;

/**
 * Redis 分布式锁工具类
 * 
 * 基于 Redis Lua 脚本实现，保证原子性操作
 * 
 * 核心特性：
 * 1. 使用 SETNX + EXPIRE 原子操作获取锁
 * 2. 锁自动过期，防止死锁
 * 3. 使用 UUID 标识锁持有者，只能释放自己持有的锁
 * 4. 支持锁重入（可选）
 * 5. 支持锁重试机制
 */
@Slf4j
@Component
public class RedisDistributedLock {

    @Resource
    private RedisUtils redisUtils;

    /**
     * 锁前缀
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 默认锁过期时间（毫秒）- 30秒
     */
    private static final long DEFAULT_EXPIRE_TIME = 30 * 1000L;

    /**
     * 默认重试间隔（毫秒）- 100毫秒
     */
    private static final long DEFAULT_RETRY_INTERVAL = 100L;

    /**
     * Lua 脚本：获取锁（SET NX PX）
     * 
     * KEYS[1]: 锁Key
     * ARGV[1]: 锁值（UUID）
     * ARGV[2]: 过期时间（毫秒）
     * 
     * 返回值：1表示获取成功，0表示获取失败
     */
    private static final String LOCK_SCRIPT =
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then\n" +
            "    redis.call('pexpire', KEYS[1], ARGV[2])\n" +
            "    return 1\n" +
            "end\n" +
            "return 0";

    /**
     * Lua 脚本：释放锁（只有锁持有者才能释放）
     * 
     * KEYS[1]: 锁Key
     * ARGV[1]: 锁值（UUID）
     * 
     * 返回值：1表示释放成功，0表示锁不存在或不是当前持有者
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "    redis.call('del', KEYS[1])\n" +
            "    return 1\n" +
            "end\n" +
            "return 0";

    /**
     * Lua 脚本：获取锁并支持重入
     * 
     * KEYS[1]: 锁Key
     * ARGV[1]: 锁值（UUID）
     * ARGV[2]: 过期时间（毫秒）
     * 
     * 返回值：1表示获取成功（首次获取或重入），0表示获取失败
     */
    private static final String LOCK_REENTRANT_SCRIPT =
            "local current = redis.call('get', KEYS[1])\n" +
            "if current == ARGV[1] then\n" +
            "    -- 重入：更新过期时间\n" +
            "    redis.call('pexpire', KEYS[1], ARGV[2])\n" +
            "    return 1\n" +
            "elseif current == false then\n" +
            "    -- 首次获取\n" +
            "    if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then\n" +
            "        redis.call('pexpire', KEYS[1], ARGV[2])\n" +
            "        return 1\n" +
            "    end\n" +
            "end\n" +
            "return 0";

    /**
     * 获取锁Key
     */
    private String getLockKey(String key) {
        return LOCK_PREFIX + key;
    }

    /**
     * 生成锁值（UUID）
     */
    private String generateLockValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取分布式锁（默认参数）
     * 
     * @param key 锁的标识（如：order:123）
     * @return 锁值（UUID），获取失败返回 null
     */
    public String tryLock(String key) {
        return tryLock(key, DEFAULT_EXPIRE_TIME, 0, DEFAULT_RETRY_INTERVAL, false);
    }

    /**
     * 获取分布式锁（指定过期时间）
     * 
     * @param key       锁的标识
     * @param expireMs  锁过期时间（毫秒）
     * @return 锁值（UUID），获取失败返回 null
     */
    public String tryLock(String key, long expireMs) {
        return tryLock(key, expireMs, 0, DEFAULT_RETRY_INTERVAL, false);
    }

    /**
     * 获取分布式锁（支持重试）
     * 
     * @param key           锁的标识
     * @param expireMs      锁过期时间（毫秒）
     * @param maxRetryTime  最大重试时间（毫秒），0表示不重试
     * @return 锁值（UUID），获取失败返回 null
     */
    public String tryLock(String key, long expireMs, long maxRetryTime) {
        return tryLock(key, expireMs, maxRetryTime, DEFAULT_RETRY_INTERVAL, false);
    }

    /**
     * 获取分布式锁（完整参数）
     * 
     * @param key           锁的标识
     * @param expireMs      锁过期时间（毫秒）
     * @param maxRetryTime  最大重试时间（毫秒），0表示不重试
     * @param retryInterval 重试间隔（毫秒）
     * @param reentrant     是否支持重入
     * @return 锁值（UUID），获取失败返回 null
     */
    public String tryLock(String key, long expireMs, long maxRetryTime, long retryInterval, boolean reentrant) {
        String lockKey = getLockKey(key);
        String lockValue = generateLockValue();
        long startTime = System.currentTimeMillis();

        while (true) {
            Object result;
            if (reentrant) {
                result = redisUtils.executeLua(LOCK_REENTRANT_SCRIPT,
                        new String[]{lockKey},
                        new String[]{lockValue, String.valueOf(expireMs)});
            } else {
                result = redisUtils.executeLua(LOCK_SCRIPT,
                        new String[]{lockKey},
                        new String[]{lockValue, String.valueOf(expireMs)});
            }

            if (result != null && "1".equals(result.toString())) {
                log.debug("获取分布式锁成功，key: {}, value: {}", key, lockValue);
                return lockValue;
            }

            // 不重试，直接返回失败
            if (maxRetryTime <= 0) {
                log.debug("获取分布式锁失败，key: {}", key);
                return null;
            }

            // 检查是否超过最大重试时间
            if (System.currentTimeMillis() - startTime >= maxRetryTime) {
                log.debug("获取分布式锁超时，key: {}, maxRetryTime: {}ms", key, maxRetryTime);
                return null;
            }

            // 等待重试
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("获取分布式锁被中断，key: {}", key);
                return null;
            }
        }
    }

    /**
     * 释放分布式锁
     * 
     * @param key       锁的标识
     * @param lockValue 锁值（UUID），用于验证锁持有者
     * @return 是否释放成功
     */
    public boolean unlock(String key, String lockValue) {
        if (lockValue == null) {
            return false;
        }

        String lockKey = getLockKey(key);
        Object result = redisUtils.executeLua(UNLOCK_SCRIPT,
                new String[]{lockKey},
                new String[]{lockValue});

        boolean success = result != null && "1".equals(result.toString());
        if (success) {
            log.debug("释放分布式锁成功，key: {}", key);
        } else {
            log.debug("释放分布式锁失败，key: {}, 可能锁已过期或不是当前持有者", key);
        }
        return success;
    }

    /**
     * 检查锁是否存在
     * 
     * @param key 锁的标识
     * @return 是否存在
     */
    public boolean isLocked(String key) {
        String lockKey = getLockKey(key);
        return redisUtils.hasKey(lockKey);
    }

    /**
     * 获取锁的剩余过期时间
     * 
     * @param key 锁的标识
     * @return 剩余时间（秒），-2表示锁不存在
     */
    public long getLockExpire(String key) {
        String lockKey = getLockKey(key);
        return redisUtils.getExpire(lockKey);
    }
}
