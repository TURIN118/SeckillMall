package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.dto.WalletRechargeRequest;
import com.seckill.mall.entity.RechargeCard;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.RechargeCardStatus;
import com.seckill.mall.mapper.RechargeCardMapper;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.RechargeCardService;
import com.seckill.mall.vo.WalletRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钱包 Controller
 * <p>
 * 前缀 {@code /api/v1/wallet}，需登录（BUYER/ADMIN）。
 * <ul>
 *   <li>查询余额：从 {@code t_user.balance} 读取</li>
 *   <li>充值：调用 {@link RechargeCardService#recharge} 完成充值卡核销与余额累加</li>
 *   <li>交易记录：当前以充值卡使用记录作为钱包交易记录</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：WalletController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "钱包", description = "余额查询/充值/交易记录")
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class WalletController {

    private final RechargeCardService rechargeCardService;
    private final com.seckill.mall.mapper.UserMapper userMapper;
    private final RechargeCardMapper rechargeCardMapper;

    @Operation(summary = "查询当前用户余额")
    @GetMapping("/balance")
    public Result<BigDecimal> balance() {
        User user = SecurityUtils.getCurrentUser();
        BigDecimal bal = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        return Result.success(bal);
    }

    @Operation(summary = "充值（通过充值卡）")
    @PostMapping("/recharge")
    public Result<BigDecimal> recharge(@Valid @RequestBody WalletRechargeRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        BigDecimal newBalance = rechargeCardService.recharge(req.getCardNo(), req.getCardPassword(), userId);
        return Result.success("充值成功", newBalance);
    }

    @Operation(summary = "交易记录（充值记录）")
    @GetMapping("/records")
    public Result<List<WalletRecordVO>> records() {
        Long userId = SecurityUtils.getCurrentUserId();
        // 查询当前用户已使用的充值卡作为交易记录
        List<RechargeCard> cards = rechargeCardMapper.selectList(
                new LambdaQueryWrapper<RechargeCard>()
                        .eq(RechargeCard::getUsedBy, userId)
                        .eq(RechargeCard::getStatus, RechargeCardStatus.USED)
                        .orderByDesc(RechargeCard::getUsedTime));
        if (cards.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        // 查询用户当前余额，用于计算每笔交易后的余额（简化：都填当前余额）
        User user = userMapper.selectById(userId);
        BigDecimal currentBalance = user == null || user.getBalance() == null
                ? BigDecimal.ZERO : user.getBalance();
        List<WalletRecordVO> list = cards.stream()
                .map(card -> toVO(card, currentBalance))
                .collect(Collectors.toList());
        return Result.success(list);
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