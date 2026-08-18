package com.seckill.mall.seckill.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.seckill.api.SeckillGoodsApi;
import com.seckill.mall.seckill.api.command.CreateSeckillGoodsCommand;
import com.seckill.mall.seckill.api.dto.SeckillGoodsDTO;
import com.seckill.mall.seckill.api.query.SeckillGoodsQuery;
import com.seckill.mall.seckill.application.facade.SeckillApiConverter;
import com.seckill.mall.service.SeckillGoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SeckillGoods 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link SeckillGoodsApi}，内部委托给旧 {@link SeckillGoodsService}，
 * 通过 {@link SeckillApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command/Query 提取业务参数，转换为旧 Request</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO/裸值 转换为 API 层 DTO/裸值</li>
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
public class SeckillGoodsApplicationService implements SeckillGoodsApi {

    private final SeckillGoodsService seckillGoodsService;

    // === 查询 ===

    @Override
    public PageResult<SeckillGoodsDTO> listSeckill(SeckillGoodsQuery query) {
        return SeckillApiConverter.toGoodsDTOPageResult(
                seckillGoodsService.listSeckill(
                        query.getStatus(), query.getCategoryId(),
                        query.getPageNum(), query.getPageSize()));
    }

    @Override
    public SeckillGoodsDTO getSeckillDetail(Long seckillId) {
        return SeckillApiConverter.toDTO(
                seckillGoodsService.getSeckillDetail(seckillId));
    }

    // === 管理 ===

    @Override
    public SeckillGoodsDTO createSeckill(CreateSeckillGoodsCommand command) {
        return SeckillApiConverter.toDTO(
                seckillGoodsService.createSeckill(toRequest(command)));
    }

    @Override
    public SeckillGoodsDTO updateSeckill(Long id, CreateSeckillGoodsCommand command) {
        return SeckillApiConverter.toDTO(
                seckillGoodsService.updateSeckill(id, toRequest(command)));
    }

    @Override
    public void cancelSeckill(Long id) {
        seckillGoodsService.cancelSeckill(id);
    }

    // === 库存 ===

    @Override
    public Integer getStock(Long seckillId) {
        return seckillGoodsService.getStock(seckillId);
    }

    @Override
    public void preheatSeckill(Long seckillId) {
        seckillGoodsService.preheatSeckill(seckillId);
    }

    // === 统计（stats 模块用）===

    @Override
    public long countAll() {
        return seckillGoodsService.countAll();
    }

    @Override
    public long countActive() {
        return seckillGoodsService.countActive();
    }

    @Override
    public long countPending() {
        return seckillGoodsService.countPending();
    }

    @Override
    public long countCompletedToday() {
        return seckillGoodsService.countCompletedToday();
    }

    /**
     * CreateSeckillGoodsCommand → SeckillCreateRequest（适配旧 Service 入参）。
     *
     * <p>Strangler Pattern 过渡期：旧 Service 接受 SeckillCreateRequest，
     * 新 API 层使用 CreateSeckillGoodsCommand，此处做反向转换以委托旧 Service。
     */
    private static SeckillCreateRequest toRequest(CreateSeckillGoodsCommand command) {
        if (command == null) {
            return null;
        }
        SeckillCreateRequest request = new SeckillCreateRequest();
        request.setProductId(command.getProductId());
        request.setSeckillPrice(command.getSeckillPrice());
        request.setStockCount(command.getTotalStock());
        request.setPerLimit(command.getLimitPerUser());
        request.setStartTime(command.getStartTime());
        request.setEndTime(command.getEndTime());
        return request;
    }
}