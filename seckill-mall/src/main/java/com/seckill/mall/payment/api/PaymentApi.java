package com.seckill.mall.payment.api;

import com.seckill.mall.payment.api.command.PayCommand;

/**
 * Payment 模块支付 API。
 *
 * <p>对外暴露支付扣款能力（钱包扣款 + 模拟支付），供 order / seckill 模块调用。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * <p>原方法映射参见 PAYMENT-API-CONTRACT.md 第 9.1 节。
 *
 * @author wnj
 * @since Phase PM.2
 */
public interface PaymentApi {

    /**
     * 执行支付扣款。
     *
     * <p>若 payMethod 为 WALLET，原子扣减用户钱包余额，余额不足抛 {@code WALLET_BALANCE_NOT_ENOUGH}；
     * 其他 payMethod 走模拟支付（直接成功，不实际扣款）。
     *
     * @param command 支付命令（userId + amount + payMethod）
     * @throws com.seckill.mall.exception.BusinessException 钱包余额不足时抛出
     */
    void pay(PayCommand command);
}