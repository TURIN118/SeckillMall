package com.seckill.mall.seckill.interfaces.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀排行榜项 VO
 *
 * <p>按销量或销售额排序的秒杀活动 Top N 排行榜中的一行，
 * 关联 t_seckill_goods 与 t_product 取商品名称。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillRankingVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillRankingVO {

    /**
     * 秒杀活动 ID（t_seckill_goods.id）
     */
    private Long seckillId;

    /**
     * 商品名称（关联 t_product.name）
     */
    private String productName;

    /**
     * 秒杀价（t_seckill_goods.seckill_price）
     */
    private BigDecimal seckillPrice;

    /**
     * 销量（关联订单数或订单中 quantity 之和；此处取有效订单数量）
     */
    private Long salesCount;

    /**
     * 销售额（SUM(total_amount)，仅统计 PAID/COMPLETED）
     */
    private BigDecimal totalAmount;
}