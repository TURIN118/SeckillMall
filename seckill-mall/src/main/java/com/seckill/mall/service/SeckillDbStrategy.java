package com.seckill.mall.service;

import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.seckill.infrastructure.mapper.SeckillGoodsMapper;
import com.seckill.mall.seckill.infrastructure.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillDbStrategy.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillDbStrategy {

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillOrderService seckillOrderService;

    /**
     * 数据库直降模式：Redis 不可用时使用。
     * 流程：乐观锁扣减库存 + 唯一索引兜底重复下单。
     *
     * @return true 扣减成功（用户未重复下单且库存充足）
     */
    public boolean deductStock(Long seckillId, Long userId) {
        // 1. 先判重：uk_user_seckill 命中即视为重复下单，直接拒绝
        SeckillOrder existed = seckillOrderMapper.findByUserAndSeckill(userId, seckillId);
        if (existed != null) {
            log.info("DB 模式命中重复下单 seckillId={} userId={}", seckillId, userId);
            return false;
        }

        // 2. 乐观锁扣减库存：available_count > 0 保证原子性与并发安全
        int affected = seckillGoodsMapper.deductStockOptimistic(seckillId);
        if (affected <= 0) {
            log.info("DB 模式库存扣减失败（售罄）seckillId={}", seckillId);
            return false;
        }
        return true;
    }

    /**
     * DB 模式完整流程：库存扣减 + 订单创建在同一事务内，避免扣减成功但下单失败的库存泄漏。
     * 失败时由调用方根据异常区分售罄/重复下单。
     */
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder executeDbModeSeckill(Long seckillId, Long userId, String requestId) {
        if (!deductStock(seckillId, userId)) {
            // 区分售罄与重复下单：deductStock 返回 false 可能是库存不足或已下单
            if (seckillOrderMapper.findByUserAndSeckill(userId, seckillId) != null) {
                throw new BusinessException(ErrorCode.REPEAT_SECKILL);
            }
            throw new BusinessException(ErrorCode.STOCK_EMPTY);
        }
        // uk_user_seckill 唯一索引作为最终并发兜底；若命中则事务回滚，库存自动恢复
        return seckillOrderService.createSeckillOrder(seckillId, userId, requestId);
    }
}
