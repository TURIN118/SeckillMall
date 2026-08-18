package com.seckill.mall.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀结果 DTO，含订单号与支付状态等完整结果信息。
 *
 * <p>与 {@link com.seckill.mall.seckill.api.result.SeckillResult} 区别：
 * SeckillResult 用于 executeSeckill/getSeckillResult 同步返回（含 requestId 用于轮询）；
 * SeckillResultDTO 用于内部传递与异步消息体，含完整订单信息。
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 5.6 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResultDTO {

    /** 是否成功 */
    private boolean success;

    /** 请求 ID */
    private String requestId;

    /** 订单 ID（成功时） */
    private Long orderId;

    /** 提示信息 */
    private String message;
}