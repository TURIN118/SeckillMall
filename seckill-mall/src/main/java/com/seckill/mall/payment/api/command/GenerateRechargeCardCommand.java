package com.seckill.mall.payment.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 批量生成充值卡命令。
 *
 * <p>原方法：{@code RechargeCardService.generate(BigDecimal faceValue, Integer count)}
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRechargeCardCommand {

    /** 面额（必填，必须大于0） */
    private BigDecimal faceValue;

    /** 生成数量（必填，1-1000） */
    private Integer count;
}