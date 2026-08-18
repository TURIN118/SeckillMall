package com.seckill.mall.payment.application;

import com.seckill.mall.payment.api.WalletApi;
import com.seckill.mall.payment.api.dto.WalletRecordDTO;
import com.seckill.mall.payment.application.facade.PaymentApiConverter;
import com.seckill.mall.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wallet 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link WalletApi}，内部委托给旧 {@link WalletService}，
 * 通过 {@link PaymentApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Query 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO 转换为 API 层 DTO</li>
 * </ol>
 *
 * @author wnj
 * @since Phase PM.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletApplicationService implements WalletApi {

    private final WalletService walletService;

    @Override
    public BigDecimal getBalance(Long userId) {
        return walletService.getBalance(userId);
    }

    @Override
    public List<WalletRecordDTO> listRecords(Long userId) {
        return PaymentApiConverter.toDTOList(walletService.listRecords(userId));
    }
}