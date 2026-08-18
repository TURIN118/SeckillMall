package com.seckill.mall.service.impl;

import com.seckill.mall.cache.SeckillLuaService;
import com.seckill.mall.seckill.infrastructure.mapper.SeckillGoodsMapper;
import com.seckill.mall.service.SeckillInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 秒杀库存端口实现：统一 DB + Redis 库存回补。
 * <p>
 * 从 {@code SeckillOrderServiceImpl.rollbackStock} 抽取（Phase 10），
 * 建立秒杀库存操作统一入口。
 * <p>
 * H-C1：在 afterCommit 中回补 DB available_count（之前只回补 Redis，DB 库存单调递减）。
 * M-C2：Redis 回补使用 Lua 脚本原子执行 INCR + SREM，避免非原子操作的不一致。
 * <p>
 * DB 回补失败仅记录日志，由 SeckillStatusScheduler 中的库存对账补偿任务兜底。
 * Redis 回补失败仅记录日志，不影响主流程。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillInventoryPortImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillInventoryPortImpl implements SeckillInventoryPort {

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillLuaService seckillLuaService;

    @Override
    public void rollback(Long seckillId, Long userId) {
        // H-C1 修复：回补 DB available_count
        try {
            int rows = seckillGoodsMapper.restoreStockOptimistic(seckillId);
            if (rows == 0) {
                log.warn("回补 DB 库存失败（活动不存在或库存已满），seckillId={}，由补偿任务兜底", seckillId);
            } else {
                log.info("回补 DB 库存成功 seckillId={}", seckillId);
            }
        } catch (Exception e) {
            log.warn("回补 DB 库存异常 seckillId={}，由补偿任务兜底", seckillId, e);
        }
        // M-C2 修复：原子回补 Redis 库存（Lua 脚本 INCR + SREM）
        try {
            seckillLuaService.rollbackDeduct(seckillId, userId);
        } catch (Exception e) {
            log.warn("回补 Redis 库存失败，由补偿任务兜底 seckillId={} userId={}", seckillId, userId, e);
        }
    }
}