package com.seckill.mall.payment.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.payment.api.RechargeCardApi;
import com.seckill.mall.payment.api.command.DisableRechargeCardCommand;
import com.seckill.mall.payment.api.command.GenerateRechargeCardCommand;
import com.seckill.mall.payment.api.command.RechargeCommand;
import com.seckill.mall.payment.api.dto.RechargeCardDTO;
import com.seckill.mall.payment.api.dto.RechargeCardGenerateDTO;
import com.seckill.mall.payment.api.query.RechargeCardQuery;
import com.seckill.mall.payment.api.result.RechargeResult;
import com.seckill.mall.payment.application.facade.PaymentApiConverter;
import com.seckill.mall.service.RechargeCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * RechargeCard 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link RechargeCardApi}，内部委托给旧 {@link RechargeCardService}，
 * 通过 {@link PaymentApiConverter} 做 VO/Entity ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command/Query 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO/Entity/BigDecimal 转换为 API 层 DTO/Result</li>
 * </ol>
 *
 * @author wnj
 * @since Phase PM.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeCardApplicationService implements RechargeCardApi {

    private final RechargeCardService rechargeCardService;

    @Override
    public List<RechargeCardGenerateDTO> generate(GenerateRechargeCardCommand command) {
        return PaymentApiConverter.toGenerateDTOList(
                rechargeCardService.generate(command.getFaceValue(), command.getCount()));
    }

    @Override
    public RechargeResult recharge(RechargeCommand command) {
        BigDecimal newBalance = rechargeCardService.recharge(
                command.getCardNo(), command.getCardPassword(), command.getUserId());
        return PaymentApiConverter.toRechargeResult(newBalance);
    }

    @Override
    public PageResult<RechargeCardDTO> listPage(RechargeCardQuery query) {
        return PaymentApiConverter.toRechargeCardDTOPageResult(
                rechargeCardService.listPage(query.getPageNum(), query.getPageSize(),
                        query.getBatchNo(), query.getStatus()));
    }

    @Override
    public void disable(DisableRechargeCardCommand command) {
        rechargeCardService.disable(command.getId());
    }

    @Override
    public List<RechargeCardDTO> getUsedCardsByUser(Long userId) {
        return PaymentApiConverter.toRechargeCardDTOListFromEntity(
                rechargeCardService.getUsedCardsByUser(userId));
    }
}