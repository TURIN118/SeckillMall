package com.seckill.mall.service.impl;

import com.seckill.mall.payment.infrastructure.entity.RechargeCard;
import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.identity.api.dto.UserSnapshot;
import com.seckill.mall.order.api.OrderQueryApi;
import com.seckill.mall.order.api.result.OrderSnapshot;
import com.seckill.mall.service.RechargeCardService;
import com.seckill.mall.service.SeckillOrderService;
import com.seckill.mall.service.WalletService;
import com.seckill.mall.payment.interfaces.vo.WalletRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 钱包服务实现
 * <p>
 * M-D2 修复：从 {@code WalletController} 下沉而来，封装对
 * {@code RechargeCardMapper} / {@code UserMapper} 的访问，
 * Controller 不再直接依赖 Mapper。
 * <p>
 * Phase 12：消除跨模块 Mapper 依赖，改用对应领域 Service 调用，
 * 本类不再直接依赖 UserMapper / RechargeCardMapper / NormalOrderMapper / SeckillOrderMapper。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：WalletServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserApi userApi;
    private final RechargeCardService rechargeCardService;
    private final OrderQueryApi orderQueryApi;
    private final SeckillOrderService seckillOrderService;

    @Override
    public BigDecimal getBalance(Long userId) {
        UserSnapshot user = userApi.getUserById(userId);
        if (user == null || user.getBalance() == null) {
            return BigDecimal.ZERO;
        }
        return user.getBalance();
    }

    @Override
    public List<WalletRecordVO> listRecords(Long userId) {
        // 查询用户当前余额，用于填充每笔交易后的余额（简化：都填当前余额）
        UserSnapshot user = userApi.getUserById(userId);
        BigDecimal currentBalance = user == null || user.getBalance() == null
                ? BigDecimal.ZERO : user.getBalance();

        List<WalletRecordVO> records = new ArrayList<>();

        // 1. 查询当前用户已使用的充值卡作为充值记录（type=RECHARGE，金额为正）
        List<RechargeCard> cards = rechargeCardService.getUsedCardsByUser(userId);
        for (RechargeCard card : cards) {
            records.add(toRechargeVO(card, currentBalance));
        }

        // 2. 查询普通订单中钱包支付的已支付订单作为消费记录（type=CONSUME，金额为负）
        List<OrderSnapshot> orderSnapshots = orderQueryApi.getWalletPaidOrders(userId);
        for (OrderSnapshot snap : orderSnapshots) {
            records.add(toConsumeVO(snap.getOrderId(), snap.getPayAmount(),
                    snap.getPayTime(), snap.getCreateTime(), currentBalance, "订单支付"));
        }

        // 3. 查询秒杀订单中钱包支付的已支付订单作为消费记录
        List<SeckillOrder> seckillOrders = seckillOrderService.getWalletPaidOrdersByUser(userId);
        for (SeckillOrder order : seckillOrders) {
            records.add(toConsumeVO(order.getId(), order.getTotalAmount(),
                    order.getPayTime(), order.getCreateTime(), currentBalance, "秒杀订单支付"));
        }

        // 合并后按交易时间倒序排序（null 排最后）
        records.sort(Comparator.comparing(WalletRecordVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return records;
    }

    /** RechargeCard → WalletRecordVO（充值，金额为正） */
    private WalletRecordVO toRechargeVO(RechargeCard card, BigDecimal balanceAfter) {
        WalletRecordVO vo = new WalletRecordVO();
        vo.setId(card.getId());
        vo.setType("RECHARGE");
        vo.setAmount(card.getFaceValue());
        vo.setBalanceAfter(balanceAfter);
        vo.setCreateTime(card.getUsedTime());
        // 安全修复（L3）：卡号脱敏，仅显示后四位，避免敏感信息泄露
        String cardNo = card.getCardNo();
        String maskedCardNo = (cardNo == null || cardNo.length() < 4)
                ? "****"
                : "****" + cardNo.substring(cardNo.length() - 4);
        vo.setRemark("充值卡充值，卡号：" + maskedCardNo);
        return vo;
    }

    /**
     * 订单支付 → WalletRecordVO（消费，金额为负数表示出账）
     *
     * @param orderId      订单ID
     * @param payAmount    实付金额（正数）
     * @param payTime      支付时间
     * @param createTime   订单创建时间（payTime 为空时回退使用）
     * @param balanceAfter 交易后余额
     * @param remark       备注信息
     */
    private WalletRecordVO toConsumeVO(Long orderId, BigDecimal payAmount,
                                       LocalDateTime payTime, LocalDateTime createTime,
                                       BigDecimal balanceAfter, String remark) {
        WalletRecordVO vo = new WalletRecordVO();
        vo.setId(orderId);
        vo.setType("CONSUME");
        // 消费金额取负数表示出账
        BigDecimal amount = payAmount == null ? BigDecimal.ZERO : payAmount.negate();
        vo.setAmount(amount);
        vo.setBalanceAfter(balanceAfter);
        // 优先使用支付时间，缺失时回退到订单创建时间
        vo.setCreateTime(payTime != null ? payTime : createTime);
        vo.setRemark(remark);
        return vo;
    }
}
