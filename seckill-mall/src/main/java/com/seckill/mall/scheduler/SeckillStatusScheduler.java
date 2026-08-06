package com.seckill.mall.scheduler;

import com.seckill.mall.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bug6修复：秒杀状态定时调度器。
 * <p>
 * DB 中秒杀活动的 status 字段在创建时设为 PENDING，需要定时任务根据
 * start_time/end_time 自动更新为 ACTIVE 或 ENDED，确保首页与秒杀专区状态一致。
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
}