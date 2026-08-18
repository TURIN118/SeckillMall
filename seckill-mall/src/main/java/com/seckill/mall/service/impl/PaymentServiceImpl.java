package com.seckill.mall.service.impl;

import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付服务实现：处理订单支付的扣款逻辑。
 * <p>
 * Phase 4b-2：从 {@code OrderServiceImpl.payNormalOrder} 与
 * {@code SeckillOrderServiceImpl.payOrder} 抽取的重复支付逻辑，
 * 统一钱包扣款与模拟支付的判定与日志，建立 Payment 模块边界。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：PaymentServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** 钱包支付方式标识 */
    private static final String PAY_METHOD_WALLET = "WALLET";

    private final UserApi userApi;

    @Override
    public void pay(Long userId, BigDecimal amount, String payMethod) {
        if (PAY_METHOD_WALLET.equalsIgnoreCase(payMethod)) {
            // 钱包支付：原子扣减余额，余额不足提示去充值
            int rows = userApi.deductBalance(userId, amount);
            if (rows == 0) {
                throw new BusinessException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
            }
            log.info("钱包扣款成功，userId={}, amount={}, payMethod={}", userId, amount, payMethod);
        } else {
            // 模拟支付（ALIPAY/WECHAT 等）：直接成功，不实际扣款
            log.info("模拟支付成功，userId={}, amount={}, payMethod={}", userId, amount, payMethod);
        }
    }
}