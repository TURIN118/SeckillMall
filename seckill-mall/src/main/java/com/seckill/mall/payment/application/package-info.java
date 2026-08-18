/**
 * Payment 模块应用层 - 用例编排与门面委托。
 *
 * <p>包含 PaymentApplicationService、WalletApplicationService、RechargeCardApplicationService，
 * 实现 API 接口并委托旧 Service（Strangler Pattern 过渡期）。
 *
 * @author wnj
 * @since Phase PM.0
 */
package com.seckill.mall.payment.application;