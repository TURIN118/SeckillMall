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
     * @throws IllegalArgumentException 若 seckillId/userId 为空或脚本未初始化
     */
    public Long deductStock(Long seckillId, Long userId, long boughtSetTtlSeconds) {
        // L25 修复：添加空值检查，避免 NPE 在 Lua 执行时难以定位
        if (seckillId == null) {
            throw new IllegalArgumentException("seckillId 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (deductScript == null) {
            throw new IllegalStateException("deductScript 未初始化，请检查 @PostConstruct 是否执行");
        }
        String stockKey = RedisKeyConstants.seckillStock(seckillId);
        String boughtKey = RedisKeyConstants.seckillBought(seckillId);
        List<String> keys = List.of(stockKey, boughtKey);
        return redisTemplate.execute(deductScript, keys, String.valueOf(userId), String.valueOf(boughtSetTtlSeconds));
    }

    /**
     * H12: 回补 Lua 预减库存并移除已购标记，用于 MQ 发送失败时的库存泄漏修复。
     * 操作非原子（回补库存 + 移除 bought 集合元素），仅在异常回滚路径调用，可接受。
     */
    public void rollbackDeduct(Long seckillId, Long userId) {
        String stockKey = RedisKeyConstants.seckillStock(seckillId);
        String boughtKey = RedisKeyConstants.seckillBought(seckillId);
        // 回补库存
        redisTemplate.opsForValue().increment(stockKey);
        // 移除已购标记
        redisTemplate.opsForSet().remove(boughtKey, String.valueOf(userId));
    }
}
