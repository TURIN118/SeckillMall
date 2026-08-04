package com.seckill.mall.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RedisService.java
 * 邮箱：nj651217@163.com
 */
@Component
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }

    public Long del(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long decr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long sRem(String key, String... values) {
        return redisTemplate.opsForSet().remove(key, (Object[]) values);
    }

    public Boolean sIsMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public void hSet(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public String hGet(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return value == null ? null : value.toString();
    }

    public Map<String, String> hGetAll(String key) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        if (raw.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>(raw.size());
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            result.put(entry.getKey() == null ? null : entry.getKey().toString(),
                    entry.getValue() == null ? null : entry.getValue().toString());
        }
        return result;
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 使用 SCAN 游标迭代安全遍历 Key，避免 KEYS 阻塞主线程。
     */
    public List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }
}
