package com.seckill.mall.seckill.application;

import com.seckill.mall.seckill.api.SeckillApi;
import com.seckill.mall.seckill.api.command.SeckillCommand;
import com.seckill.mall.seckill.api.result.SeckillResult;
import com.seckill.mall.seckill.application.facade.SeckillApiConverter;
import com.seckill.mall.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Seckill 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link SeckillApi}，内部委托给旧 {@link SeckillService}，
 * 通过 {@link SeckillApiConverter} 做 VO ↔ Result 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 SeckillResultVO 转换为 API 层 SeckillResult</li>
 * </ol>
 *
 * <p>委托映射参见 SECKILL-API-CONTRACT.md 第 9 节。
 *
 * @author wnj
 * @since Phase SK.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillApplicationService implements SeckillApi {

    private final SeckillService seckillService;

    @Override
    public SeckillResult executeSeckill(SeckillCommand command) {
        return SeckillApiConverter.toResult(
                seckillService.doSeckill(command.getSeckillId(), command.getSeckillToken()));
    }

    @Override
    public SeckillResult getSeckillResult(Long seckillId, String requestId) {
        return SeckillApiConverter.toResult(
                seckillService.getSeckillResult(seckillId, requestId));
    }
}