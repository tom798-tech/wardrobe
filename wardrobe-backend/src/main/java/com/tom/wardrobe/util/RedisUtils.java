package com.tom.wardrobe.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 常用操作工具类（基于 RedisTemplate 二次封装）
 * 使用方式：在任意 Bean 中 @Autowired RedisUtils 即可
 */
@Component
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /* ==================== 基础 Key 操作 ==================== */

    public boolean expire(String key, long time) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, time, TimeUnit.SECONDS));
    }

    public long getExpire(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null ? -2 : ttl;
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) Arrays.asList(key));
            }
        }
    }

    /* ==================== String 操作 ==================== */

    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        Long val = redisTemplate.opsForValue().increment(key, delta);
        return val == null ? 0 : val;
    }

    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        Long val = redisTemplate.opsForValue().decrement(key, delta);
        return val == null ? 0 : val;
    }

    /* ==================== Hash 操作 ==================== */

    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    public boolean hHasKey(String key, String item) {
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, item));
    }

    /* ==================== Set 操作 ==================== */

    public Set<Object> sGet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public boolean sHasKey(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    public long sSet(String key, Object... values) {
        Long count = redisTemplate.opsForSet().add(key, values);
        return count == null ? 0 : count;
    }

    public long sSetAndTime(String key, long time, Object... values) {
        Long count = redisTemplate.opsForSet().add(key, values);
        if (time > 0) expire(key, time);
        return count == null ? 0 : count;
    }

    public long sGetSetSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    public long setRemove(String key, Object... values) {
        Long count = redisTemplate.opsForSet().remove(key, values);
        return count == null ? 0 : count;
    }

    /* ==================== List 操作 ==================== */

    public List<Object> lGet(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public long lGetListSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0 : size;
    }

    public Object lGetIndex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long lRemove(String key, long count, Object value) {
        Long removed = redisTemplate.opsForList().remove(key, count, value);
        return removed == null ? 0 : removed;
    }

    /* ==================== ZSet 操作（有序集合） ==================== */

    /**
     * 添加元素到有序集合
     *
     * @param key   键
     * @param score 分数
     * @param value 值
     * @return 是否添加成功
     */
    public boolean zadd(String key, double score, Object value) {
        try {
            Boolean result = redisTemplate.opsForZSet().add(key, value, score);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取有序集合的大小
     *
     * @param key 键
     * @return 集合大小
     */
    public long zcard(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size == null ? 0 : size;
    }

    /**
     * 移除分数在指定范围内的元素
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 移除的元素数量
     */
    public long zremrangebyscore(String key, double min, double max) {
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
        return removed == null ? 0 : removed;
    }

    /**
     * 获取分数在指定范围内的元素数量
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素数量
     */
    public long zcount(String key, double min, double max) {
        Long count = redisTemplate.opsForZSet().count(key, min, max);
        return count == null ? 0 : count;
    }

    /**
     * 获取指定范围的元素
     *
     * @param key   键
     * @param start 起始索引
     * @param end   结束索引
     * @return 元素列表
     */
    public Set<Object> zrange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /* ==================== Lua 脚本操作 ==================== */

    /**
     * 执行 Lua 脚本
     *
     * @param script   Lua 脚本内容
     * @param keys     Redis Key 列表
     * @param args     参数列表
     * @return 脚本执行结果
     */
    public Object executeLua(String script, String[] keys, String[] args) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        List<String> keyList = Arrays.asList(keys);
        return redisTemplate.execute(
                redisScript,
                RedisSerializer.string(),
                new GenericToStringSerializer<>(Long.class),
                keyList,
                (Object[]) args
        );
    }
}
