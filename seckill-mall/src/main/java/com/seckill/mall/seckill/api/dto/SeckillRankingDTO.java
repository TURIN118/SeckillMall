package com.seckill.mall.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 秒杀排行榜 DTO，用于 stats 模块销售排行展示。
 *
 * <p>包含秒杀商品 ID、名称、销售数量、销售总额四项指标。
 *
 * <p>来源映射：
 * <ul>
 *     <li>SeckillRankingVO → SeckillRankingDTO（{@code SeckillApiConverter.toDTO}）</li>
 * </ul>
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 5.5 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillRankingDTO {

    /** 秒杀商品 ID */
    private Long seckillId;

    /** 秒杀商品名称 */
    private String seckillName;

    /** 销售数量 */
    private long salesCount;

    /** 销售总额 */
    private BigDecimal salesAmount;
}