package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.dto.WalletRechargeRequest;
import com.seckill.mall.identity.infrastructure.entity.User;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.RechargeCardService;
import com.seckill.mall.service.WalletService;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包 Controller
 * <p>
 * 前缀 {@code /api/v1/wallet}，需登录（BUYER/ADMIN）。
 * <ul>
 *   <li>查询余额：从 {@code t_user.balance} 读取</li>
 *   <li>充值：调用 {@link RechargeCardService#recharge} 完成充值卡核销与余额累加</li>
 *   <li>交易记录：当前以充值卡使用记录作为钱包交易记录</li>
 * </ul>
 * <p>
 * M-D2 修复：移除对 {@code RechargeCardMapper} / {@code UserMapper} 的直接依赖，
 * 全部下沉到 {@link WalletService}，Controller 仅编排并返回 {@code Result<VO>}。
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
    private final WalletService walletService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "查询当前用户余额")
    @GetMapping("/balance")
    public Result<BigDecimal> balance() {
        User user = securityUtils.getCurrentUser();
        BigDecimal bal = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        return Result.success(bal);
    }

    @Operation(summary = "充值（通过充值卡）")
    @PostMapping("/recharge")
    public Result<BigDecimal> recharge(@Valid @RequestBody WalletRechargeRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        BigDecimal newBalance = rechargeCardService.recharge(req.getCardNo(), req.getCardPassword(), userId);
        return Result.success("充值成功", newBalance);
    }

    @Operation(summary = "交易记录（充值记录）")
    @GetMapping("/records")
    public Result<List<WalletRecordVO>> records() {
        Long userId = securityUtils.getCurrentUserId();
        List<WalletRecordVO> list = walletService.listRecords(userId);
        return Result.success(list);
    }
}
