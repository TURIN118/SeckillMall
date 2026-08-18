package com.seckill.mall.seckill.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀结果，{@code SeckillApi.executeSeckill} 与 {@code SeckillApi.getSeckillResult} 返回值。
 *
 * <p>含 {@code requestId} 用于前端轮询秒杀结果：前端调用 executeSeckill 拿到 requestId 后，
 * 周期性调用 getSeckillResult(seckillId, requestId) 直到 success=true 或超时。
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 8.1 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResult {

    /** 是否成功 */
    private boolean success;

    /** 请求 ID（用于轮询） */
    private String requestId;

    /** 订单 ID（成功时） */
    private Long orderId;

    /** 提示信息 */
    private String message;
}