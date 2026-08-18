package com.seckill.mall.seckill.infrastructure.cache;

import com.seckill.mall.cache.RedisKeyConstants;
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
    private DefaultRedisScript<Long> rollbackScript;

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_deduct.lua")));
        deductScript.setResultType(Long.class);

        // M-C2 修复：加载原子回补脚本
        rollbackScript = new DefaultRedisScript<>();
        rollbackScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_rollback.lua")));
        rollbackScript.setResultType(Long.class);
    }

    /**
     * 原子预减库存。
     *
     * @param boughtSetTtlSeconds 已购集合 TTL（活动剩余秒数，>0 时设置）
     * @param stockTtlSeconds      库存 Key TTL（活动剩余秒数，M-C3 修复：补传给 Lua）
     * @return 1=成功 / -1=库存不足 / -2=重复下单
     * @throws IllegalArgumentException 若 seckillId/userId 为空或脚本未初始化
     */
    public Long deductStock(Long seckillId, Long userId, long boughtSetTtlSeconds, long stockTtlSeconds) {
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
        return redisTemplate.execute(deductScript, keys,
                String.valueOf(userId),
                String.valueOf(boughtSetTtlSeconds),
                String.valueOf(stockTtlSeconds));
    }

    /**
     * M-C3 兼容：保留三参数重载，stockTtl 默认取 boughtSetTtl（活动剩余时间）。
     * 新调用方应使用四参数重载显式传 stockTtl。
     */
    public Long deductStock(Long seckillId, Long userId, long boughtSetTtlSeconds) {
        return deductStock(seckillId, userId, boughtSetTtlSeconds, boughtSetTtlSeconds);
    }

    /**
     * H12 / M-C2 修复：原子回补 Lua 预减库存并移除已购标记。
     * <p>
     * 使用 Lua 脚本原子执行 INCR(stockKey) + SREM(boughtKey, userId)，
     * 避免非原子操作在并发场景下的库存不一致。
     *
     * @return 1=成功
     */
    public Long rollbackDeduct(Long seckillId, Long userId) {
        if (seckillId == null || userId == null) {
            throw new IllegalArgumentException("seckillId/userId 不能为空");
        }
        if (rollbackScript == null) {
            throw new IllegalStateException("rollbackScript 未初始化，请检查 @PostConstruct 是否执行");
        }
        String stockKey = RedisKeyConstants.seckillStock(seckillId);
        String boughtKey = RedisKeyConstants.seckillBought(seckillId);
        List<String> keys = List.of(stockKey, boughtKey);
        return redisTemplate.execute(rollbackScript, keys, String.valueOf(userId));
    }
}
