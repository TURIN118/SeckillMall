package com.seckill.mall.seckill.application.facade;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.SeckillActivityCreateRequest;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.seckill.api.command.CreateActivityCommand;
import com.seckill.mall.seckill.api.command.CreateSeckillGoodsCommand;
import com.seckill.mall.seckill.api.dto.SeckillActivityDTO;
import com.seckill.mall.seckill.api.dto.SeckillGoodsDTO;
import com.seckill.mall.seckill.api.dto.SeckillOrderDTO;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.seckill.api.result.SeckillResult;
import com.seckill.mall.seckill.domain.SeckillStatus;
import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.seckill.interfaces.vo.SeckillActivityVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillGoodsVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillOrderVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillRankingVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Seckill API 转换辅助类。
 *
 * <p>集中存放旧 VO/Entity 与新 API 层 DTO/Command 之间的转换方法，
 * 供 SeckillApplicationService / SeckillOrderApplicationService /
 * SeckillActivityApplicationService / SeckillGoodsApplicationService / Controller 调用。
 * 所有方法均为无状态静态方法，标注 {@code @Component} 仅为便于未来扩展为 Bean 注入方式。
 *
 * <p>转换原则：
 * <ul>
 *     <li>Entity → DTO：枚举字段取 getCode()，丢弃 isDeleted 等基础设施字段</li>
 *     <li>VO → DTO：核心字段映射，枚举取 ordinal() 适配 DTO 的 Integer status</li>
 *     <li>DTO → VO：核心字段映射，Integer status 还原为 SeckillStatus 枚举</li>
 *     <li>Request → Command：字段一一对应，丢弃 Bean Validation 注解</li>
 *     <li>SeckillResultVO → SeckillResult：status=1 映射 success=true，其余 false</li>
 * </ul>
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 10 节。
 *
 * @author wnj
 * @since Phase SK.4
 */
@Slf4j
@Component
public class SeckillApiConverter {

    // ============================================================
    // SeckillResultVO → SeckillResult（ApplicationService 用）
    // ============================================================

    /**
     * SeckillResultVO → SeckillResult。
     *
     * <p>旧 VO status 语义：0=排队中 / 1=成功 / -1=失败。
     * 新 Result success 语义：status=1 → true，其余 → false。
     */
    public static SeckillResult toResult(SeckillResultVO vo) {
        if (vo == null) {
            return null;
        }
        boolean success = vo.getStatus() != null && vo.getStatus() == 1;
        String message = success ? "秒杀成功" : (vo.getStatus() != null && vo.getStatus() == 0 ? "排队中" : "秒杀失败");
        return SeckillResult.builder()
                .success(success)
                .requestId(vo.getRequestId())
                .orderId(vo.getOrderId())
                .message(message)
                .build();
    }

    // ============================================================
    // SeckillResult → SeckillResultVO（Controller 用，反向转换）
    // ============================================================

    /** SeckillResult → SeckillResultVO（Controller 层前端契约适配）。 */
    public static SeckillResultVO toVO(SeckillResult result) {
        if (result == null) {
            return null;
        }
        SeckillResultVO vo = new SeckillResultVO();
        vo.setStatus(result.isSuccess() ? 1 : -1);
        vo.setRequestId(result.getRequestId());
        vo.setOrderId(result.getOrderId());
        return vo;
    }

    // ============================================================
    // SeckillActivityVO ↔ SeckillActivityDTO 转换
    // ============================================================

    /** SeckillActivityVO → SeckillActivityDTO */
    public static SeckillActivityDTO toDTO(SeckillActivityVO vo) {
        if (vo == null) {
            return null;
        }
        return SeckillActivityDTO.builder()
                .id(vo.getId())
                .name(vo.getName())
                .startTime(vo.getStartTime())
                .endTime(vo.getEndTime())
                .status(vo.getStatus())
                .goodsList(toGoodsDTOList(vo.getGoodsList()))
                .build();
    }

    /** SeckillActivityVO 列表 → SeckillActivityDTO 列表 */
    public static List<SeckillActivityDTO> toActivityDTOList(List<SeckillActivityVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(SeckillApiConverter::toDTO).collect(Collectors.toList());
    }

    /** SeckillActivityDTO → SeckillActivityVO（Controller 层前端契约适配） */
    public static SeckillActivityVO toVO(SeckillActivityDTO dto) {
        if (dto == null) {
            return null;
        }
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setStartTime(dto.getStartTime());
        vo.setEndTime(dto.getEndTime());
        vo.setStatus(dto.getStatus());
        vo.setGoodsList(toGoodsVOList(dto.getGoodsList()));
        return vo;
    }

    /** SeckillActivityDTO 列表 → SeckillActivityVO 列表 */
    public static List<SeckillActivityVO> toActivityVOList(List<SeckillActivityDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(SeckillApiConverter::toVO).collect(Collectors.toList());
    }

    // ============================================================
    // SeckillGoodsVO ↔ SeckillGoodsDTO 转换
    // ============================================================

    /** SeckillGoodsVO → SeckillGoodsDTO */
    public static SeckillGoodsDTO toDTO(SeckillGoodsVO vo) {
        if (vo == null) {
            return null;
        }
        return SeckillGoodsDTO.builder()
                .id(vo.getId())
                .productId(vo.getProductId())
                .productName(vo.getProductName())
                .seckillPrice(vo.getSeckillPrice())
                .totalStock(vo.getStockCount())
                .availableStock(vo.getAvailableCount())
                .startTime(vo.getStartTime())
                .endTime(vo.getEndTime())
                .status(vo.getStatus() == null ? null : vo.getStatus().ordinal())
                .limitPerUser(vo.getPerLimit())
                .build();
    }

    /** SeckillGoodsVO 列表 → SeckillGoodsDTO 列表 */
    public static List<SeckillGoodsDTO> toGoodsDTOList(List<SeckillGoodsVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(SeckillApiConverter::toDTO).collect(Collectors.toList());
    }

    /** SeckillGoodsDTO → SeckillGoodsVO（Controller 层前端契约适配） */
    public static SeckillGoodsVO toVO(SeckillGoodsDTO dto) {
        if (dto == null) {
            return null;
        }
        SeckillGoodsVO vo = new SeckillGoodsVO();
        vo.setId(dto.getId());
        vo.setProductId(dto.getProductId());
        vo.setProductName(dto.getProductName());
        vo.setSeckillPrice(dto.getSeckillPrice());
        vo.setStockCount(dto.getTotalStock());
        vo.setAvailableCount(dto.getAvailableStock());
        vo.setStartTime(dto.getStartTime());
        vo.setEndTime(dto.getEndTime());
        vo.setStatus(integerToSeckillStatus(dto.getStatus()));
        vo.setPerLimit(dto.getLimitPerUser());
        return vo;
    }

    /** SeckillGoodsDTO 列表 → SeckillGoodsVO 列表 */
    public static List<SeckillGoodsVO> toGoodsVOList(List<SeckillGoodsDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(SeckillApiConverter::toVO).collect(Collectors.toList());
    }

    /** PageResult<SeckillGoodsVO> → PageResult<SeckillGoodsDTO> */
    public static PageResult<SeckillGoodsDTO> toGoodsDTOPageResult(PageResult<SeckillGoodsVO> page) {
        if (page == null) {
            return null;
        }
        return PageResult.of(
                toGoodsDTOList(page.getList()),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize());
    }

    /** PageResult<SeckillGoodsDTO> → PageResult<SeckillGoodsVO> */
    public static PageResult<SeckillGoodsVO> toGoodsVOPageResult(PageResult<SeckillGoodsDTO> page) {
        if (page == null) {
            return null;
        }
        return PageResult.of(
                toGoodsVOList(page.getList()),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize());
    }

    // ============================================================
    // SeckillOrderVO ↔ SeckillOrderDTO 转换
    // ============================================================

    /** SeckillOrderVO → SeckillOrderDTO */
    public static SeckillOrderDTO toDTO(SeckillOrderVO vo) {
        if (vo == null) {
            return null;
        }
        return SeckillOrderDTO.builder()
                .id(vo.getId())
                .orderNo(vo.getOrderNo())
                .userId(vo.getUserId())
                .seckillId(vo.getSeckillId())
                .productId(vo.getProductId())
                .quantity(vo.getQuantity())
                .seckillPrice(vo.getSeckillPrice())
                .payAmount(vo.getTotalAmount())
                .payMethod(vo.getPayMethod())
                .status(vo.getStatus())
                .createTime(vo.getCreateTime())
                .payTime(vo.getPayTime())
                .shipTime(vo.getShipTime())
                .confirmTime(vo.getConfirmTime())
                .shippingCompany(vo.getShippingCompany())
                .shippingNo(vo.getShippingNo())
                .transactionId(vo.getTransactionId())
                .build();
    }

    /** SeckillOrderVO 列表 → SeckillOrderDTO 列表 */
    public static List<SeckillOrderDTO> toOrderDTOListFromVO(List<SeckillOrderVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(SeckillApiConverter::toDTO).collect(Collectors.toList());
    }

    /** SeckillOrderDTO → SeckillOrderVO（Controller 层前端契约适配） */
    public static SeckillOrderVO toVO(SeckillOrderDTO dto) {
        if (dto == null) {
            return null;
        }
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(dto.getId());
        vo.setOrderNo(dto.getOrderNo());
        vo.setUserId(dto.getUserId());
        vo.setSeckillId(dto.getSeckillId());
        vo.setProductId(dto.getProductId());
        vo.setQuantity(dto.getQuantity());
        vo.setSeckillPrice(dto.getSeckillPrice());
        vo.setTotalAmount(dto.getPayAmount());
        vo.setPayMethod(dto.getPayMethod());
        vo.setStatus(dto.getStatus());
        vo.setCreateTime(dto.getCreateTime());
        vo.setPayTime(dto.getPayTime());
        vo.setShipTime(dto.getShipTime());
        vo.setConfirmTime(dto.getConfirmTime());
        vo.setShippingCompany(dto.getShippingCompany());
        vo.setShippingNo(dto.getShippingNo());
        vo.setTransactionId(dto.getTransactionId());
        return vo;
    }

    /** SeckillOrderDTO 列表 → SeckillOrderVO 列表 */
    public static List<SeckillOrderVO> toOrderVOList(List<SeckillOrderDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(SeckillApiConverter::toVO).collect(Collectors.toList());
    }

    /** PageResult<SeckillOrderVO> → PageResult<SeckillOrderDTO> */
    public static PageResult<SeckillOrderDTO> toOrderDTOPageResult(PageResult<SeckillOrderVO> page) {
        if (page == null) {
            return null;
        }
        return PageResult.of(
                toOrderDTOListFromVO(page.getList()),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize());
    }

    /** PageResult<SeckillOrderDTO> → PageResult<SeckillOrderVO>（Controller 层前端契约适配） */
    public static PageResult<SeckillOrderVO> toOrderVOPageResult(PageResult<SeckillOrderDTO> page) {
        if (page == null) {
            return null;
        }
        return PageResult.of(
                toOrderVOList(page.getList()),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize());
    }

    // ============================================================
    // SeckillOrder Entity → SeckillOrderDTO 转换
    // ============================================================

    /** SeckillOrder Entity → SeckillOrderDTO */
    public static SeckillOrderDTO toDTO(SeckillOrder entity) {
        if (entity == null) {
            return null;
        }
        return SeckillOrderDTO.builder()
                .id(entity.getId())
                .orderNo(entity.getOrderNo())
                .userId(entity.getUserId())
                .seckillId(entity.getSeckillId())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .seckillPrice(entity.getSeckillPrice())
                .payAmount(entity.getTotalAmount())
                .payMethod(entity.getPayMethod())
                .status(entity.getStatus() == null ? null : entity.getStatus().getCode())
                .createTime(entity.getCreateTime())
                .payTime(entity.getPayTime())
                .shipTime(entity.getShipTime())
                .confirmTime(entity.getConfirmTime())
                .shippingCompany(entity.getShippingCompany())
                .shippingNo(entity.getShippingNo())
                .transactionId(entity.getTransactionId())
                .build();
    }

    /** SeckillOrder Entity 列表 → SeckillOrderDTO 列表 */
    public static List<SeckillOrderDTO> toOrderDTOListFromEntity(List<SeckillOrder> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(SeckillApiConverter::toDTO).collect(Collectors.toList());
    }

    // ============================================================
    // SeckillRankingVO → SeckillRankingDTO 转换
    // ============================================================

    /** SeckillRankingVO → SeckillRankingDTO */
    public static SeckillRankingDTO toDTO(SeckillRankingVO vo) {
        if (vo == null) {
            return null;
        }
        return SeckillRankingDTO.builder()
                .seckillId(vo.getSeckillId())
                .seckillName(vo.getProductName())
                .salesCount(vo.getSalesCount() == null ? 0L : vo.getSalesCount())
                .salesAmount(vo.getTotalAmount())
                .build();
    }

    /** SeckillRankingVO 列表 → SeckillRankingDTO 列表 */
    public static List<SeckillRankingDTO> toRankingDTOList(List<SeckillRankingVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(SeckillApiConverter::toDTO).collect(Collectors.toList());
    }

    /** SeckillRankingDTO → SeckillRankingVO（stats 模块 Controller 前端契约适配） */
    public static SeckillRankingVO toVO(SeckillRankingDTO dto) {
        if (dto == null) {
            return null;
        }
        SeckillRankingVO vo = new SeckillRankingVO();
        vo.setSeckillId(dto.getSeckillId());
        vo.setProductName(dto.getSeckillName());
        vo.setSalesCount(dto.getSalesCount());
        vo.setTotalAmount(dto.getSalesAmount());
        return vo;
    }

    /** SeckillRankingDTO 列表 → SeckillRankingVO 列表 */
    public static List<SeckillRankingVO> toRankingVOList(List<SeckillRankingDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(SeckillApiConverter::toVO).collect(Collectors.toList());
    }

    // ============================================================
    // Request → Command 转换
    // ============================================================

    /** SeckillActivityCreateRequest → CreateActivityCommand */
    public static CreateActivityCommand toCommand(SeckillActivityCreateRequest request) {
        if (request == null) {
            return null;
        }
        return CreateActivityCommand.builder()
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .goodsList(toGoodsCommandListFromActivity(request.getGoodsItems()))
                .build();
    }

    /** ActivityGoodsItem 列表 → CreateSeckillGoodsCommand 列表（场次创建用） */
    private static List<CreateSeckillGoodsCommand> toGoodsCommandListFromActivity(
            List<SeckillActivityCreateRequest.ActivityGoodsItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> CreateSeckillGoodsCommand.builder()
                        .productId(item.getProductId())
                        .seckillPrice(item.getSeckillPrice())
                        .totalStock(item.getStockCount())
                        .build())
                .collect(Collectors.toList());
    }

    /** SeckillCreateRequest → CreateSeckillGoodsCommand */
    public static CreateSeckillGoodsCommand toCommand(SeckillCreateRequest request) {
        if (request == null) {
            return null;
        }
        return CreateSeckillGoodsCommand.builder()
                .productId(request.getProductId())
                .seckillPrice(request.getSeckillPrice())
                .totalStock(request.getStockCount())
                .limitPerUser(request.getPerLimit())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /** Integer → SeckillStatus（按 ordinal 还原，越界返回 null） */
    private static SeckillStatus integerToSeckillStatus(Integer status) {
        if (status == null) {
            return null;
        }
        SeckillStatus[] values = SeckillStatus.values();
        if (status < 0 || status >= values.length) {
            return null;
        }
        return values[status];
    }
}