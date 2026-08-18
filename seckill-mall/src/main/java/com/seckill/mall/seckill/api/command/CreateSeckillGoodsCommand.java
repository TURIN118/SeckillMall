package com.seckill.mall.seckill.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建/更新秒杀商品命令。
 *
 * <p>原方法：{@code SeckillGoodsService.createSeckill(...)} / {@code updateSeckill(...)}
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 6.5 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeckillGoodsCommand {

    /** 原商品 ID（必填） */
    private Long productId;

    /** 秒杀价格（必填） */
    private BigDecimal seckillPrice;

    /** 总库存（必填） */
    private Integer totalStock;

    /** 每人限购（必填） */
    private Integer limitPerUser;

    /** 开始时间（必填） */
    private LocalDateTime startTime;

    /** 结束时间（必填） */
    private LocalDateTime endTime;

    /** 所属场次 ID（创建场次时由父命令回填；独立创建时必填） */
    private Long activityId;
}