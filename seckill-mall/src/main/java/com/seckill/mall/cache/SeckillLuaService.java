package com.seckill.mall.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillLuaService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
public class SeckillLuaService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> deductScript;

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_deduct.lua")));
        deductScript.setResultType(Long.class);
    }

    /**
     * 原子预减库存。
     *
     * @param boughtSetTtlSeconds 已购集合 TTL（活动剩余秒数，>0 时设置）
     * @return 1=成功 / -1=库存不足 / -2=重复下单
     */
    public Long deductStock(Long seckillId, Long userId, long boughtSetTtlSeconds) {
        String stockKey = RedisKeyConstants.seckillStock(seckillId);
        String boughtKey = RedisKeyConstants.seckillBought(seckillId);
        List<String> keys = List.of(stockKey, boughtKey);
        return redisTemplate.execute(deductScript, keys, String.valueOf(userId), String.valueOf(boughtSetTtlSeconds));
    }
}
