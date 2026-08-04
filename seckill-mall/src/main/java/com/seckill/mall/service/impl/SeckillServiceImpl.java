package com.seckill.mall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.CacheDegradeService;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.cache.SeckillLuaService;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.SeckillStatus;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import com.seckill.mall.mq.producer.SeckillOrderProducer;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.SeckillDbStrategy;
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
    private final SeckillOrderProducer seckillOrderProducer;
    private final CacheDegradeService cacheDegradeService;
    private final SeckillDbStrategy seckillDbStrategy;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public SeckillResultVO doSeckill(Long seckillId, String seckillToken) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 容错降级：Redis 不可用时切换为数据库直降模式，跳过缓存校验与 Lua 预减
        if (!cacheDegradeService.isRedisAvailable()) {
            log.warn("Redis 不可用，秒杀降级为数据库模式 seckillId={} userId={}", seckillId, userId);
            return doSeckillViaDb(seckillId, userId);
        }

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
        // M13: 时间解析增加异常处理，避免 NPE / DateTimeParseException 导致 500
        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = LocalDateTime.parse(info.get("startTime"), DATE_TIME_FORMATTER);
            endTime = LocalDateTime.parse(info.get("endTime"), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.error("秒杀活动时间解析失败 seckillId={} info={}", seckillId, info, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "秒杀活动数据异常");
        }

        // C4 修复：先检查是否已取消；后续仅按时间窗口判断，不依赖 DB status 字段
        // （DB status 不会随时间自动更新为 ACTIVE/ENDED，旧逻辑会导致活动开始后仍被拒绝）
        if (SeckillStatus.CANCELLED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.SECKILL_TOO_MANY);
        }
        if (now.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(endTime)) {
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

        // 投递秒杀下单消息到 RabbitMQ，由消费者异步创建订单并回写结果
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setSeckillId(seckillId);
        message.setUserId(userId);
        message.setRequestId(requestId);
        message.setTimestamp(System.currentTimeMillis());

        // MQ 宕机降级：sendSeckillOrder 返回非空表示已同步创建订单，需回写成功结果
        // H12 修复：MQ 发送异常时回补 Lua 预减库存与结果缓存，避免库存泄漏
        SeckillOrder syncOrder;
        try {
            syncOrder = seckillOrderProducer.sendSeckillOrder(message);
        } catch (Exception e) {
            log.error("MQ 发送秒杀订单失败，回补库存 seckillId={} userId={}", seckillId, userId, e);
            // 回补 Lua 预减库存（若 seckillLuaService 无 rollbackDeduct 方法，需新增该方法）
            try {
                seckillLuaService.rollbackDeduct(seckillId, userId);
            } catch (Exception ex) {
                log.error("回补 Lua 库存失败 seckillId={} userId={}", seckillId, userId, ex);
            }
            redisService.del(RedisKeyConstants.seckillResult(seckillId, userId));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        if (syncOrder != null) {
            return writeSyncSuccessResult(seckillId, userId, requestId, syncOrder);
        }

        return vo;
    }

    /**
     * 数据库直降模式：Redis 不可用时，从 DB 校验活动状态并执行乐观锁扣减 + 同步下单。
     * 令牌校验依赖 Redis，降级模式下跳过（库存与唯一索引仍可保证安全）。
     */
    private SeckillResultVO doSeckillViaDb(Long seckillId, Long userId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        // C4 修复：仅按时间窗口判断，不依赖 DB status 字段（status 不会随时间自动更新）
        if (SeckillStatus.CANCELLED.equals(goods.getStatus())) {
            throw new BusinessException(ErrorCode.SECKILL_TOO_MANY);
        }
        if (now.isBefore(goods.getStartTime())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(goods.getEndTime())) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        SeckillOrder order = seckillDbStrategy.executeDbModeSeckill(seckillId, userId, requestId);
        return writeSyncSuccessResult(seckillId, userId, requestId, order);
    }

    /**
     * 同步下单成功后回写结果到 Redis（若 Redis 可用）并返回成功 VO。
     */
    private SeckillResultVO writeSyncSuccessResult(Long seckillId, Long userId, String requestId, SeckillOrder order) {
        SeckillResultVO successVo = new SeckillResultVO();
        successVo.setStatus(1);
        successVo.setRequestId(requestId);
        successVo.setOrderId(order.getId());
        successVo.setOrderNo(order.getOrderNo());
        successVo.setTotalAmount(order.getTotalAmount());
        successVo.setPayExpireTime(order.getPayExpireTime());
        try {
            String json = objectMapper.writeValueAsString(successVo);
            redisService.set(RedisKeyConstants.seckillResult(seckillId, userId), json,
                    RESULT_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis 异常不影响已下单结果返回
            log.error("同步下单结果回写 Redis 失败 seckillId={} userId={}", seckillId, userId, e);
        }
        return successVo;
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
