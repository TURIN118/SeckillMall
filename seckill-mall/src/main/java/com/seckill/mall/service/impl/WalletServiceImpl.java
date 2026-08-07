package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.mall.entity.RechargeCard;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.RechargeCardStatus;
import com.seckill.mall.mapper.RechargeCardMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.WalletService;
import com.seckill.mall.vo.WalletRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钱包服务实现
 * <p>
 * M-D2 修复：从 {@code WalletController} 下沉而来，封装对
 * {@code RechargeCardMapper} / {@code UserMapper} 的访问，
 * Controller 不再直接依赖 Mapper。
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

    private final RechargeCardMapper rechargeCardMapper;
    private final UserMapper userMapper;

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getBalance() == null) {
            return BigDecimal.ZERO;
        }
        return user.getBalance();
    }

    @Override
    public List<WalletRecordVO> listRecords(Long userId) {
        // 查询当前用户已使用的充值卡作为交易记录
        List<RechargeCard> cards = rechargeCardMapper.selectList(
                new LambdaQueryWrapper<RechargeCard>()
                        .eq(RechargeCard::getUsedBy, userId)
                        .eq(RechargeCard::getStatus, RechargeCardStatus.USED)
                        .orderByDesc(RechargeCard::getUsedTime));
        if (cards.isEmpty()) {
            return Collections.emptyList();
        }
        // 查询用户当前余额，用于计算每笔交易后的余额（简化：都填当前余额）
        User user = userMapper.selectById(userId);
        BigDecimal currentBalance = user == null || user.getBalance() == null
                ? BigDecimal.ZERO : user.getBalance();
        return cards.stream()
                .map(card -> toVO(card, currentBalance))
                .collect(Collectors.toList());
    }

    /** RechargeCard → WalletRecordVO */
    private WalletRecordVO toVO(RechargeCard card, BigDecimal balanceAfter) {
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
}