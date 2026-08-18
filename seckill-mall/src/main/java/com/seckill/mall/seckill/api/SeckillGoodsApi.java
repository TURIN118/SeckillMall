package com.seckill.mall.seckill.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.seckill.api.command.CreateSeckillGoodsCommand;
import com.seckill.mall.seckill.api.dto.SeckillGoodsDTO;
import com.seckill.mall.seckill.api.query.SeckillGoodsQuery;

/**
 * Seckill 模块秒杀商品 API。
 *
 * <p>对外暴露秒杀商品能力（查询/创建/更新/取消/库存/预热/统计），
 * 供 SeckillController 和 stats 模块调用。
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
public interface SeckillGoodsApi {

    // === 查询 ===

    /**
     * 秒杀商品分页列表。
     *
     * @param query 查询条件（status + categoryId + pageNum + pageSize）
     * @return 分页结果
     */
    PageResult<SeckillGoodsDTO> listSeckill(SeckillGoodsQuery query);

    /**
     * 秒杀商品详情。
     *
     * @param seckillId 秒杀商品 ID
     * @return 秒杀商品 DTO
     */
    SeckillGoodsDTO getSeckillDetail(Long seckillId);

    // === 管理 ===

    /**
     * 创建秒杀商品。
     *
     * @param command 创建命令
     * @return 秒杀商品 DTO
     */
    SeckillGoodsDTO createSeckill(CreateSeckillGoodsCommand command);

    /**
     * 更新秒杀商品。
     *
     * @param id      秒杀商品 ID
     * @param command 创建命令（复用为更新入参）
     * @return 更新后的秒杀商品 DTO
     */
    SeckillGoodsDTO updateSeckill(Long id, CreateSeckillGoodsCommand command);

    /**
     * 取消秒杀商品。
     *
     * @param id 秒杀商品 ID
     */
    void cancelSeckill(Long id);

    // === 库存 ===

    /**
     * 查询库存。
     *
     * @param seckillId 秒杀商品 ID
     * @return 可用库存
     */
    Integer getStock(Long seckillId);

    /**
     * 预热秒杀商品（Redis + 布隆过滤器）。
     *
     * @param seckillId 秒杀商品 ID
     */
    void preheatSeckill(Long seckillId);

    // === 统计（stats 模块用）===

    /**
     * 秒杀活动总数。
     *
     * @return 秒杀活动总数
     */
    long countAll();

    /**
     * 进行中秒杀数。
     *
     * @return 进行中秒杀数
     */
    long countActive();

    /**
     * 待开始秒杀数。
     *
     * @return 待开始秒杀数
     */
    long countPending();

    /**
     * 今日已完成秒杀数。
     *
     * @return 今日已完成秒杀数
     */
    long countCompletedToday();
}