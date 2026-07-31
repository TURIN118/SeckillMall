package com.seckill.mall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.cache.SeckillLuaService;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.enums.SeckillStatus;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.SeckillGoodsService;
import com.seckill.mall.service.SeckillService;
import com.seckill.mall.service.SeckillTokenService;
import com.seckill.mall.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final long RESULT_TTL_MINUTES = 10L;

    private final SeckillGoodsService seckillGoodsService;
    private final SeckillTokenService seckillTokenService;
    private final SeckillLuaService seckillLuaService;
    private final RedisService redisService;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public SeckillResultVO doSeckill(Long seckillId, String seckillToken) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 1. 校验秒杀令牌
        if (!seckillTokenService.validateSeckillToken(seckillId, userId, seckillToken)) {
            throw new BusinessException(ErrorCode.SECKILL_TOKEN_INVALID);
        }

        // 2. 校验活动状态与时间窗口（Redis Hash），未预热则兜底加载
        Map<String, String> info = redisService.hGetAll(RedisKeyConstants.seckillInfo(seckillId));
        if (info.isEmpty()) {
            RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_BLOOM_GOODS);
            if (!bloomFilter.isExists() || !bloomFilter.contains(seckillId)) {
                throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
            }
            seckillGoodsService.preheatSeckill(seckillId);
            info = redisService.hGetAll(RedisKeyConstants.seckillInfo(seckillId));
            if (info.isEmpty()) {
                throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
            }
        }

        String status = info.get("status");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = LocalDateTime.parse(info.get("startTime"), DATE_TIME_FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(info.get("endTime"), DATE_TIME_FORMATTER);

        if (SeckillStatus.CANCELLED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.SECKILL_TOO_MANY);
        }
        if (now.isBefore(startTime) || SeckillStatus.PENDING.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(endTime) || SeckillStatus.ENDED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }

        // 3. Lua 原子预减库存 + 判重（传入 bought 集合 TTL = 活动剩余时间）
        long boughtTtlSeconds = Math.max(60L, java.time.Duration.between(now, endTime).getSeconds());
        Long result = seckillLuaService.deductStock(seckillId, userId, boughtTtlSeconds);
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "库存预减异常");
        }
        if (result == -1L) {
            throw new BusinessException(ErrorCode.STOCK_EMPTY);
        }
        if (result == -2L) {
            throw new BusinessException(ErrorCode.REPEAT_SECKILL);
        }

        // 4. 生成 requestId，写入秒杀结果（排队中）
        String requestId = UUID.randomUUID().toString().replace("-", "");
        SeckillResultVO vo = new SeckillResultVO();
        vo.setStatus(0);
        vo.setRequestId(requestId);

        try {
            String json = objectMapper.writeValueAsString(vo);
            redisService.set(RedisKeyConstants.seckillResult(seckillId, userId), json,
                    RESULT_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("写入秒杀结果失败 seckillId={} userId={}", seckillId, userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "秒杀结果写入失败");
        }

        // TODO: M6 实现 RabbitMQ 投递 SeckillOrderMessage，由消费者异步创建订单并回写结果
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setSeckillId(seckillId);
        message.setUserId(userId);
        message.setRequestId(requestId);

        return vo;
    }

    @Override
    public SeckillResultVO getSeckillResult(Long seckillId, String requestId) {
        Long userId = SecurityUtils.getCurrentUserId();
        String json = redisService.get(RedisKeyConstants.seckillResult(seckillId, userId));

        SeckillResultVO vo = new SeckillResultVO();
        vo.setRequestId(requestId);
        vo.setStatus(-1);

        if (json == null) {
            return vo;
        }
        try {
            SeckillResultVO cached = objectMapper.readValue(json, SeckillResultVO.class);
            if (requestId != null && !requestId.equals(cached.getRequestId())) {
                return vo;
            }
            return cached;
        } catch (Exception e) {
            log.warn("解析秒杀结果失败 seckillId={} userId={}", seckillId, userId, e);
            return vo;
        }
    }
}
