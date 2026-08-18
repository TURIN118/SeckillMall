package com.seckill.mall.payment.application;

import com.seckill.mall.payment.api.PaymentApi;
import com.seckill.mall.payment.api.command.PayCommand;
import com.seckill.mall.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Payment 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link PaymentApi}，内部委托给旧 {@link PaymentService}。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 * </ol>
 *
 * @author wnj
 * @since Phase PM.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService implements PaymentApi {

    private final PaymentService paymentService;

    @Override
    public void pay(PayCommand command) {
        paymentService.pay(command.getUserId(), command.getAmount(), command.getPayMethod());
    }
}