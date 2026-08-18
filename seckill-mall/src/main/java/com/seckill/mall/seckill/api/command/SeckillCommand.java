package com.seckill.mall.seckill.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀下单命令。
 *
 * <p>userId 从 CurrentUserContext 获取，不在 Command 中显式传递。
 *
 * <p>原方法：{@code SeckillService.doSeckill(Long seckillId, String seckillToken)}
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 6.1 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillCommand {

    /** 秒杀商品 ID（必填） */
    private Long seckillId;

    /** 秒杀令牌（必填，由 preheatSeckill 预生成） */
    private String seckillToken;
}