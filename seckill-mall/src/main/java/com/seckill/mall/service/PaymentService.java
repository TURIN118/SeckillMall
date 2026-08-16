package com.seckill.mall.service;

import java.math.BigDecimal;

/**
 * 支付服务：处理订单支付的扣款逻辑。
 * <p>
 * 当前支持 WALLET（钱包扣款）和模拟支付（ALIPAY/WECHAT 等）。
 * 未来可扩展为 PaymentGateway + Adapter 模式（微信支付/支付宝/第三方支付）。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：PaymentService.java
 * 邮箱：nj651217@163.com
 */
public interface PaymentService {

    /**
     * 执行支付扣款。
     * <p>
     * 若 payMethod 为 WALLET，原子扣减用户钱包余额，余额不足抛 {@code WALLET_BALANCE_NOT_ENOUGH}；
     * 其他 payMethod 走模拟支付（直接成功，不实际扣款）。
     *
     * @param userId    用户 ID
     * @param amount    支付金额
     * @param payMethod 支付方式（WALLET/ALIPAY/WECHAT 等）
     * @throws com.seckill.mall.exception.BusinessException 钱包余额不足时抛出
     */
    void pay(Long userId, BigDecimal amount, String payMethod);
}