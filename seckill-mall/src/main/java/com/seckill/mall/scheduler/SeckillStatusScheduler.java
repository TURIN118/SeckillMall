package com.seckill.mall.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.enums.SeckillStatus;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bug6修复 + H-C1修复：秒杀状态与库存对账定时调度器。
 * <p>
 * DB 中秒杀活动的 status 字段在创建时设为 PENDING，需要定时任务根据
 * start_time/end_time 自动更新为 ACTIVE 或 ENDED，确保首页与秒杀专区状态一致。
 * <p>
 * H-C1 修复：新增库存对账补偿任务，将 Redis 库存校正到 DB available_count 真相值，
 * 兜底因 rollbackStock 失败或并发问题导致的 Redis/DB 库存不一致。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillStatusScheduler.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SeckillStatusScheduler {

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisService redisService;

    /**
     * 每60秒扫描一次，将已到开始时间的PENDING活动更新为ACTIVE，
     * 将已过结束时间的ACTIVE活动更新为ENDED。
     */
    @Scheduled(fixedRate = 60000)
    public void updateSeckillStatus() {
        try {
            int pendingRows = seckillGoodsMapper.updatePendingToActive();
            if (pendingRows > 0) {
                log.info("秒杀状态调度：PENDING→ACTIVE，影响行数={}", pendingRows);
            }
            int activeRows = seckillGoodsMapper.updateActiveToEnded();
            if (activeRows > 0) {
                log.info("秒杀状态调度：ACTIVE→ENDED，影响行数={}", activeRows);
            }
        } catch (Exception e) {
            log.error("秒杀状态定时调度异常", e);
        }
    }

    /**
     * H-C1 修复：库存对账补偿任务，每5分钟执行一次。
     * <p>
     * 遍历所有未结束的秒杀活动，将 Redis 库存（seckill:stock:{id}）校正到
     * DB available_count 真相值。兜底以下场景：
     * <ul>
     *   <li>rollbackStock 中 Redis 回补失败（网络异常等）</li>
     *   <li>并发取消/超时导致 Redis 库存多回补</li>
     *   <li>消费者同步 Redis 失败</li>
     * </ul>
     * <p>
     * 对账策略：以 DB available_count 为唯一真相来源，Redis 仅作闸门。
     * 此处简化为直接信任 DB available_count 字段
     * （由 deductStockOptimistic/restoreStockOptimistic 维护）。
     */
    @Scheduled(fixedRate = 300000)
    public void reconcileStock() {
        try {
            // 查询所有未结束的秒杀活动（PENDING/ACTIVE）
            LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<SeckillGoods>()
                    .in(SeckillGoods::getStatus, SeckillStatus.PENDING, SeckillStatus.ACTIVE)
                    .eq(SeckillGoods::getIsDeleted, 0);
            List<SeckillGoods> activeGoods = seckillGoodsMapper.selectList(wrapper);
            if (activeGoods.isEmpty()) {
                return;
            }

            int reconciled = 0;
            for (SeckillGoods goods : activeGoods) {
                try {
                    reconcileSingleStock(goods);
                    reconciled++;
                } catch (Exception e) {
                    log.warn("库存对账单活动失败 seckillId={}", goods.getId(), e);
                }
            }
            if (reconciled > 0) {
                log.info("库存对账补偿任务完成，校正活动数={}", reconciled);
            }
        } catch (Exception e) {
            log.error("库存对账补偿任务异常", e);
        }
    }

    /**
     * 对账单个秒杀活动库存：将 Redis 校正到 DB available_count。
     */
    private void reconcileSingleStock(SeckillGoods goods) {
        Long seckillId = goods.getId();
        Integer dbStock = goods.getAvailableCount();
        if (dbStock == null) {
            return;
        }
        String stockKey = RedisKeyConstants.seckillStock(seckillId);
        String redisStockStr = redisService.get(stockKey);
        if (redisStockStr == null) {
            // Redis 无库存缓存，跳过（可能未预热）
            return;
        }
        try {
            int redisStock = Integer.parseInt(redisStockStr);
            if (redisStock != dbStock) {
                // 校正 Redis 库存到 DB 真相值
                redisService.set(stockKey, String.valueOf(dbStock));
                redisService.hSet(RedisKeyConstants.seckillInfo(seckillId), "stock", String.valueOf(dbStock));
                log.warn("库存对账校正 seckillId={} redisStock={} dbStock={}", seckillId, redisStock, dbStock);
            }
        } catch (NumberFormatException e) {
            log.warn("库存对账 Redis 库存格式异常 seckillId={} value={}", seckillId, redisStockStr);
        }
    }
}

