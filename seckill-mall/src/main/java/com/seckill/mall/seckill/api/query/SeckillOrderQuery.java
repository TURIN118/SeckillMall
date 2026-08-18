package com.seckill.mall.seckill.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀订单分页查询条件（用户端订单列表）。
 *
 * <p>原方法：{@code SeckillOrderService.getOrderList(...)}
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 7.1 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderQuery {

    /** 用户 ID（必填） */
    private Long userId;

    /** 订单状态（null=不筛选） */
    private Integer status;

    /** 页码（默认1） */
    private Integer pageNum;

    /** 每页大小（默认10） */
    private Integer pageSize;
}