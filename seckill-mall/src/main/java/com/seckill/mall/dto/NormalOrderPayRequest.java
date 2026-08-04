package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 普通订单支付请求
 * <p>
 * 支付方式为 {@code WALLET} 时使用钱包余额支付，余额不足提示去充值；
 * 其他支付方式（如 ALIPAY/WECHAT）走模拟支付直接置为 PAID。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrderPayRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class NormalOrderPayRequest {

    /** 支付方式：WALLET/ALIPAY/WECHAT 等 */
    @NotBlank(message = "支付方式不能为空")
    private String payMethod;
}