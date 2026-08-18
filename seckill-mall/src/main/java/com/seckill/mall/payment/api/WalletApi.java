package com.seckill.mall.payment.api;

import com.seckill.mall.payment.api.dto.WalletRecordDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payment 模块钱包 API。
 *
 * <p>对外暴露钱包能力（余额查询 + 交易记录），供 payment 模块 Controller 调用。
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
 * <p>原方法映射参见 PAYMENT-API-CONTRACT.md 第 9.2 节。
 *
 * @author wnj
 * @since Phase PM.2
 */
public interface WalletApi {

    /**
     * 查询指定用户余额。
     *
     * @param userId 用户 ID
     * @return 余额（用户不存在或余额为空时返回 {@code BigDecimal.ZERO}）
     */
    BigDecimal getBalance(Long userId);

    /**
     * 查询指定用户的钱包交易记录。
     *
     * <p>合并充值卡充值记录（RECHARGE，金额为正）+ 普通订单钱包支付记录（CONSUME，金额为负）
     * + 秒杀订单钱包支付记录（CONSUME，金额为负），按交易时间倒序。
     *
     * @param userId 用户 ID
     * @return 交易记录列表（DTO，不含敏感信息，卡号已脱敏）
     */
    List<WalletRecordDTO> listRecords(Long userId);
}