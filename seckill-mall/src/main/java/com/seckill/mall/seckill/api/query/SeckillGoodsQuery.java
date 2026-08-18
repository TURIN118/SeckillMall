package com.seckill.mall.seckill.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀商品分页查询条件（后台管理列表）。
 *
 * <p>原方法：{@code SeckillGoodsService.listSeckill(...)}
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 7.2 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsQuery {

    /** 状态筛选（可空） */
    private String status;

    /** 分类 ID 筛选（可空） */
    private Long categoryId;

    /** 页码（默认1） */
    private Integer pageNum;

    /** 每页大小（默认10） */
    private Integer pageSize;
}