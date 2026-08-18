package com.seckill.mall.seckill.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建秒杀场次命令（含场次下所有商品）。
 *
 * <p>原方法：{@code SeckillActivityService.createActivity(...)}
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 6.4 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityCommand {

    /** 场次名称（必填） */
    private String name;

    /** 开始时间（必填） */
    private LocalDateTime startTime;

    /** 结束时间（必填） */
    private LocalDateTime endTime;

    /** 场次下商品列表（必填，至少一项） */
    private List<CreateSeckillGoodsCommand> goodsList;
}