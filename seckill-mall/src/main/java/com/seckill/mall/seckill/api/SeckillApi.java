package com.seckill.mall.seckill.api;

import com.seckill.mall.seckill.api.command.SeckillCommand;
import com.seckill.mall.seckill.api.result.SeckillResult;

/**
 * Seckill 模块秒杀核心 API。
 *
 * <p>对外暴露秒杀核心能力（执行秒杀 + 查询秒杀结果），供 SeckillController 调用。
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
 * <p>原方法映射参见 SECKILL-API-CONTRACT.md 第 9 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
public interface SeckillApi {

    /**
     * 执行秒杀下单。
     *
     * <p>令牌校验 → 库存预扣（Redis Lua 原子操作）→ 异步发消息创建订单。
     *
     * @param command 秒杀命令（seckillId + seckillToken，userId 从 CurrentUserContext 获取）
     * @return 秒杀结果（含 requestId，用于轮询秒杀结果）
     */
    SeckillResult executeSeckill(SeckillCommand command);

    /**
     * 查询秒杀结果（前端轮询用）。
     *
     * @param seckillId 秒杀商品 ID
     * @param requestId 请求 ID（executeSeckill 返回的）
     * @return 秒杀结果（含订单号、支付状态等）
     */
    SeckillResult getSeckillResult(Long seckillId, String requestId);
}