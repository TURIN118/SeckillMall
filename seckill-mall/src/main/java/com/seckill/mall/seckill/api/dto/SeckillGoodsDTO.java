package com.seckill.mall.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品 DTO，替代 SeckillGoodsVO 跨模块与跨层传递。
 *
 * <p>包含秒杀商品完整字段（核心 + 库存 + 时间 + 状态），供 Application 层返回与 Controller 层转回 VO 使用。
 *
 * <p>来源映射：
 * <ul>
 *     <li>SeckillGoodsVO → SeckillGoodsDTO（{@code SeckillApiConverter.toDTO}）</li>
 * </ul>
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 5.2 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsDTO {

    /** 秒杀商品 ID */
    private Long id;

    /** 原商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品主图 */
    private String productImage;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 总库存 */
    private Integer totalStock;

    /** 可用库存 */
    private Integer availableStock;

    /** 每人限购 */
    private Integer limitPerUser;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 状态 */
    private Integer status;

    /** 所属场次 ID */
    private Long activityId;
}