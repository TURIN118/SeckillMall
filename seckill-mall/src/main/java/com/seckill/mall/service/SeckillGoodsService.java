package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.seckill.interfaces.vo.SeckillGoodsVO;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillGoodsService.java
 * 邮箱：nj651217@163.com
 */
public interface SeckillGoodsService {

    PageResult<SeckillGoodsVO> listSeckill(String status, Long categoryId, Integer pageNum, Integer pageSize);

    SeckillGoodsVO getSeckillDetail(Long seckillId);

    SeckillGoodsVO createSeckill(SeckillCreateRequest req);

    SeckillGoodsVO updateSeckill(Long id, SeckillCreateRequest req);

    void cancelSeckill(Long id);

    Integer getStock(Long seckillId);

    void preheatSeckill(Long seckillId);

    /**
     * Phase 14：秒杀活动总数（封装 seckillGoodsMapper.selectCount(null)，消除跨模块 Mapper 依赖）。
     *
     * @return 秒杀活动总数
     */
    long countAll();

    /**
     * Phase 14：进行中秒杀数（基于时间窗口动态计算：start_time &lt;= now &lt; end_time 且 status != CANCELLED）。
     * <p>
     * M17: DB 中 status 字段不会随时间自动更新，直接按 status=ACTIVE 查询会漏掉已开始但 status 仍为 PENDING 的活动。
     *
     * @return 进行中秒杀数
     */
    long countActive();

    /**
     * Phase 14：待开始秒杀数（基于时间窗口：start_time &gt; now 且 status != CANCELLED）。
     *
     * @return 待开始秒杀数
     */
    long countPending();

    /**
     * Phase 14：今日已完成秒杀数（基于时间窗口：end_time &lt; now 且 endTime 在今日、status != CANCELLED）。
     *
     * @return 今日已完成秒杀数
     */
    long countCompletedToday();
}
