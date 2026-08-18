package com.seckill.mall.service;

import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.seckill.infrastructure.entity.SeckillGoods;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillTokenService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillTokenService {

    private final RedisService redisService;
    private final SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 生成秒杀令牌，TTL 为活动剩余时间。
     */
    public String getSeckillToken(Long seckillId, Long userId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            return null;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttl = Math.max(30L, Duration.between(LocalDateTime.now(), goods.getEndTime()).getSeconds());
        redisService.set(RedisKeyConstants.seckillToken(seckillId, userId), token, ttl, TimeUnit.SECONDS);
        return token;
    }

    public boolean validateSeckillToken(Long seckillId, Long userId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String cached = redisService.get(RedisKeyConstants.seckillToken(seckillId, userId));
        return token.equals(cached);
    }
}
