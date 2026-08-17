package com.seckill.mall.order.application.facade;

import com.seckill.mall.dto.AdminOrderQueryRequest;
import com.seckill.mall.order.api.dto.AdminOrderDTO;
import com.seckill.mall.order.api.dto.OrderDTO;
import com.seckill.mall.order.api.dto.OrderDetailDTO;
import com.seckill.mall.order.api.dto.OrderItemDTO;
import com.seckill.mall.order.api.dto.OrderItemSnapshotDTO;
import com.seckill.mall.order.api.dto.OrderListItemDTO;
import com.seckill.mall.order.api.query.AdminOrderQuery;
import com.seckill.mall.order.api.result.OrderCancelResult;
import com.seckill.mall.order.api.result.OrderCreateResult;
import com.seckill.mall.order.api.result.OrderPayResult;
import com.seckill.mall.order.api.result.OrderSnapshot;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrder;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.vo.AdminOrderVO;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.NormalOrderItemVO;
import com.seckill.mall.vo.NormalOrderVO;
import com.seckill.mall.vo.OrderListItemVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order API 转换辅助类。
 *
 * <p>集中存放旧 VO 与新 API 层 DTO/Result 之间的转换方法，
 * 供 ApplicationService 调用。所有方法均为无状态静态方法，
 * 标注 {@code @Component} 仅为便于未来扩展为 Bean 注入方式。
 *
 * <p>转换原则：
 * <ul>
 *     <li>VO → Result：提取核心字段，丢弃前端展示专用字段</li>
 *     <li>VO → DTO：全字段映射，保持数据完整性</li>
 *     <li>Entity → Snapshot：仅提取跨模块传递所需字段，避免暴露 Entity</li>
 *     <li>Query → Request：API 层 Query → 旧 DTO Request，字段一一对应</li>
 * </ul>
 *
 * @author wnj
 * @since Phase 3.4-A
 */
@Component
public class OrderApiConverter {

    // ============================================================
    // NormalOrderDetailVO → Result 转换（写操作出参）
    // ============================================================

    /**
     * 将 {@link NormalOrderDetailVO} 转换为 {@link OrderCreateResult}。
     *
     * <p>提取订单创建后前端需要的核心字段。
     *
     * @param vo 订单详情 VO
     * @return 创建订单结果
     */
    public static OrderCreateResult toCreateResult(NormalOrderDetailVO vo) {
        if (vo == null || vo.getOrder() == null) {
            return null;
        }
        NormalOrderVO order = vo.getOrder();
        return OrderCreateResult.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .payDeadline(order.getPayExpireTime())
                .build();
    }

    /**
     * 将 {@link NormalOrderDetailVO} 转换为 {@link OrderPayResult}。
     *
     * <p>提取支付后前端需要的核心字段。
     *
     * @param vo 订单详情 VO
     * @return 支付订单结果
     */
    public static OrderPayResult toPayResult(NormalOrderDetailVO vo) {
        if (vo == null || vo.getOrder() == null) {
            return null;
        }
        NormalOrderVO order = vo.getOrder();
        return OrderPayResult.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus() != null ? order.getStatus().getCode() : null)
                .payMethod(order.getPayMethod())
                .transactionId(order.getTransactionId())
                .payTime(order.getPayTime())
                .build();
    }

    /**
     * 将 {@link NormalOrderDetailVO} 转换为 {@link OrderCancelResult}。
     *
     * <p>提取取消后前端需要的核心字段。
     *
     * @param vo 订单详情 VO
     * @return 取消订单结果
     */
    public static OrderCancelResult toCancelResult(NormalOrderDetailVO vo) {
        if (vo == null || vo.getOrder() == null) {
            return null;
        }
        NormalOrderVO order = vo.getOrder();
        return OrderCancelResult.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus() != null ? order.getStatus().getCode() : null)
                .cancelTime(order.getCancelTime())
                .cancelReason(order.getCancelReason())
                .build();
    }

    // ============================================================
    // NormalOrderDetailVO → OrderDetailDTO 转换（查询出参）
    // ============================================================

    /**
     * 将 {@link NormalOrderDetailVO} 转换为 {@link OrderDetailDTO}。
     *
     * <p>全字段映射，包含订单基础信息、明细列表与收货地址信息。
     *
     * @param vo 订单详情 VO
     * @return 订单详情 DTO
     */
    public static OrderDetailDTO toDetailDTO(NormalOrderDetailVO vo) {
        if (vo == null) {
            return null;
        }
        return OrderDetailDTO.builder()
                .order(toOrderDTO(vo.getOrder()))
                .items(toOrderItemDTOList(vo.getItems()))
                .receiverName(vo.getReceiverName())
                .receiverPhone(vo.getReceiverPhone())
                .province(vo.getProvince())
                .city(vo.getCity())
                .district(vo.getDistrict())
                .detailAddress(vo.getDetailAddress())
                .build();
    }

    /**
     * 将 {@link NormalOrderVO} 转换为 {@link OrderDTO}。
     *
     * @param vo 订单基础信息 VO
     * @return 订单基础信息 DTO
     */
    public static OrderDTO toOrderDTO(NormalOrderVO vo) {
        if (vo == null) {
            return null;
        }
        return OrderDTO.builder()
                .id(vo.getId())
                .orderNo(vo.getOrderNo())
                .userId(vo.getUserId())
                .addressId(vo.getAddressId())
                .totalAmount(vo.getTotalAmount())
                .freightAmount(vo.getFreightAmount())
                .payAmount(vo.getPayAmount())
                .status(vo.getStatus() != null ? vo.getStatus().getCode() : null)
                .shippingCompany(vo.getShippingCompany())
                .shippingNo(vo.getShippingNo())
                .payMethod(vo.getPayMethod())
                .transactionId(vo.getTransactionId())
                .payTime(vo.getPayTime())
                .payExpireTime(vo.getPayExpireTime())
                .cancelTime(vo.getCancelTime())
                .cancelReason(vo.getCancelReason())
                .shipTime(vo.getShipTime())
                .confirmTime(vo.getConfirmTime())
                .remark(vo.getRemark())
                .userCouponId(vo.getUserCouponId())
                .discountAmount(vo.getDiscountAmount())
                .createTime(vo.getCreateTime())
                .updateTime(vo.getUpdateTime())
                .build();
    }

    /**
     * 将 {@link NormalOrderItemVO} 列表转换为 {@link OrderItemDTO} 列表。
     *
     * @param voList 订单明细 VO 列表
     * @return 订单明细 DTO 列表
     */
    public static List<OrderItemDTO> toOrderItemDTOList(List<NormalOrderItemVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(OrderApiConverter::toOrderItemDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将 {@link NormalOrderItemVO} 转换为 {@link OrderItemDTO}。
     *
     * @param vo 订单明细 VO
     * @return 订单明细 DTO
     */
    public static OrderItemDTO toOrderItemDTO(NormalOrderItemVO vo) {
        if (vo == null) {
            return null;
        }
        return OrderItemDTO.builder()
                .id(vo.getId())
                .orderId(vo.getOrderId())
                .productId(vo.getProductId())
                .skuId(vo.getSkuId())
                .skuAttributes(vo.getSkuAttributes())
                .productName(vo.getProductName())
                .productImage(vo.getProductImage())
                .unitPrice(vo.getUnitPrice())
                .quantity(vo.getQuantity())
                .subtotal(vo.getSubtotal())
                .build();
    }

    // ============================================================
    // OrderListItemVO → OrderListItemDTO 转换
    // ============================================================

    /**
     * 将 {@link OrderListItemVO} 转换为 {@link OrderListItemDTO}。
     *
     * @param vo 订单列表项 VO
     * @return 订单列表项 DTO
     */
    public static OrderListItemDTO toListItemDTO(OrderListItemVO vo) {
        if (vo == null) {
            return null;
        }
        return OrderListItemDTO.builder()
                .id(vo.getId())
                .orderNo(vo.getOrderNo())
                .orderType(vo.getOrderType())
                .status(vo.getStatus())
                .totalAmount(vo.getTotalAmount())
                .payMethod(vo.getPayMethod())
                .createTime(vo.getCreateTime())
                .payTime(vo.getPayTime())
                .shipTime(vo.getShipTime())
                .items(toSnapshotDTOList(vo.getItems()))
                .build();
    }

    /**
     * 将 {@link OrderListItemVO.OrderItemSnapshot} 列表转换为 {@link OrderItemSnapshotDTO} 列表。
     *
     * @param voList 商品快照 VO 列表
     * @return 商品快照 DTO 列表
     */
    public static List<OrderItemSnapshotDTO> toSnapshotDTOList(List<OrderListItemVO.OrderItemSnapshot> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(OrderApiConverter::toSnapshotDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将 {@link OrderListItemVO.OrderItemSnapshot} 转换为 {@link OrderItemSnapshotDTO}。
     *
     * @param vo 商品快照 VO
     * @return 商品快照 DTO
     */
    public static OrderItemSnapshotDTO toSnapshotDTO(OrderListItemVO.OrderItemSnapshot vo) {
        if (vo == null) {
            return null;
        }
        return OrderItemSnapshotDTO.builder()
                .productId(vo.getProductId())
                .productName(vo.getProductName())
                .productImage(vo.getProductImage())
                .unitPrice(vo.getUnitPrice())
                .quantity(vo.getQuantity())
                .build();
    }

    // ============================================================
    // AdminOrderVO → AdminOrderDTO 转换
    // ============================================================

    /**
     * 将 {@link AdminOrderVO} 转换为 {@link AdminOrderDTO}。
     *
     * <p>提取管理员视角的核心字段。
     *
     * @param vo 管理员订单 VO
     * @return 管理员订单 DTO
     */
    public static AdminOrderDTO toAdminOrderDTO(AdminOrderVO vo) {
        if (vo == null) {
            return null;
        }
        return AdminOrderDTO.builder()
                .id(vo.getId())
                .orderNo(vo.getOrderNo())
                .orderType(vo.getOrderType())
                .status(vo.getStatus())
                .userId(vo.getUserId())
                .totalAmount(vo.getTotalAmount())
                .payMethod(vo.getPayMethod())
                .createTime(vo.getCreateTime())
                .payTime(vo.getPayTime())
                .shipTime(vo.getShipTime())
                .items(null)
                .build();
    }

    // ============================================================
    // NormalOrder → OrderSnapshot 转换（Entity → 快照 DTO）
    // ============================================================

    /**
     * 将 {@link NormalOrder} Entity 转换为 {@link OrderSnapshot}。
     *
     * <p>仅提取跨模块传递所需字段，避免暴露 Entity。
     *
     * @param entity 普通订单 Entity
     * @return 订单快照
     */
    public static OrderSnapshot toSnapshot(NormalOrder entity) {
        if (entity == null) {
            return null;
        }
        return OrderSnapshot.builder()
                .orderId(entity.getId())
                .orderNo(entity.getOrderNo())
                .userId(entity.getUserId())
                .payAmount(entity.getPayAmount())
                .payMethod(entity.getPayMethod())
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .payTime(entity.getPayTime())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * 将 {@link NormalOrder} Entity 列表转换为 {@link OrderSnapshot} 列表。
     *
     * @param entities 普通订单 Entity 列表
     * @return 订单快照列表
     */
    public static List<OrderSnapshot> toSnapshotList(List<NormalOrder> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(OrderApiConverter::toSnapshot)
                .collect(Collectors.toList());
    }

    // ============================================================
    // AdminOrderQuery → AdminOrderQueryRequest 转换（Query → 旧 DTO）
    // ============================================================

    /**
     * 将 API 层 {@link AdminOrderQuery} 转换为旧 {@link AdminOrderQueryRequest}。
     *
     * <p>字段一一对应，供旧 AdminOrderService 调用。
     *
     * @param query API 层管理员订单查询条件
     * @return 旧管理员订单查询请求
     */
    public static AdminOrderQueryRequest toAdminOrderQueryRequest(AdminOrderQuery query) {
        if (query == null) {
            return null;
        }
        AdminOrderQueryRequest req = new AdminOrderQueryRequest();
        req.setPageNum(query.getPageNum());
        req.setPageSize(query.getPageSize());
        req.setOrderNo(query.getOrderNo());
        req.setDate(query.getDate());
        req.setStatus(query.getStatus());
        req.setOrderType(query.getOrderType());
        req.setUserId(query.getUserId());
        req.setProductId(query.getProductId());
        req.setSeckillId(query.getSeckillId());
        req.setStartTime(query.getStartTime());
        req.setEndTime(query.getEndTime());
        req.setStartDate(query.getStartDate());
        req.setEndDate(query.getEndDate());
        req.setMinAmount(query.getMinAmount());
        req.setMaxAmount(query.getMaxAmount());
        req.setPayStartTime(query.getPayStartTime());
        req.setPayEndTime(query.getPayEndTime());
        req.setSortBy(query.getSortBy());
        req.setSortOrder(query.getSortOrder());
        return req;
    }

    // ============================================================
    // DTO → VO 反向转换（供 Controller 保持前端返回结构不变）
    // ============================================================

    /**
     * 将 {@link OrderDetailDTO} 反向转换为 {@link NormalOrderDetailVO}。
     *
     * <p>供 Controller 切换到 {@code OrderQueryApi} 后保持前端返回结构不变。
     *
     * @param dto 订单详情 DTO
     * @return 订单详情 VO
     */
    public static NormalOrderDetailVO toNormalOrderDetailVO(OrderDetailDTO dto) {
        if (dto == null) return null;
        NormalOrderDetailVO vo = new NormalOrderDetailVO();
        vo.setOrder(toNormalOrderVO(dto.getOrder()));
        vo.setItems(toNormalOrderItemVOList(dto.getItems()));
        vo.setReceiverName(dto.getReceiverName());
        vo.setReceiverPhone(dto.getReceiverPhone());
        vo.setProvince(dto.getProvince());
        vo.setCity(dto.getCity());
        vo.setDistrict(dto.getDistrict());
        vo.setDetailAddress(dto.getDetailAddress());
        return vo;
    }

    /**
     * 将 {@link OrderDTO} 反向转换为 {@link NormalOrderVO}。
     *
     * @param dto 订单基础信息 DTO
     * @return 订单基础信息 VO
     */
    public static NormalOrderVO toNormalOrderVO(OrderDTO dto) {
        if (dto == null) return null;
        NormalOrderVO vo = new NormalOrderVO();
        vo.setId(dto.getId());
        vo.setOrderNo(dto.getOrderNo());
        vo.setUserId(dto.getUserId());
        vo.setAddressId(dto.getAddressId());
        vo.setTotalAmount(dto.getTotalAmount());
        vo.setFreightAmount(dto.getFreightAmount());
        vo.setPayAmount(dto.getPayAmount());
        vo.setStatus(dto.getStatus() != null ? OrderStatus.fromCode(dto.getStatus()) : null);
        vo.setShippingCompany(dto.getShippingCompany());
        vo.setShippingNo(dto.getShippingNo());
        vo.setPayMethod(dto.getPayMethod());
        vo.setTransactionId(dto.getTransactionId());
        vo.setPayTime(dto.getPayTime());
        vo.setPayExpireTime(dto.getPayExpireTime());
        vo.setCancelTime(dto.getCancelTime());
        vo.setCancelReason(dto.getCancelReason());
        vo.setShipTime(dto.getShipTime());
        vo.setConfirmTime(dto.getConfirmTime());
        vo.setRemark(dto.getRemark());
        vo.setUserCouponId(dto.getUserCouponId());
        vo.setDiscountAmount(dto.getDiscountAmount());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    /**
     * 将 {@link OrderItemDTO} 列表反向转换为 {@link NormalOrderItemVO} 列表。
     *
     * @param dtoList 订单明细 DTO 列表
     * @return 订单明细 VO 列表
     */
    public static List<NormalOrderItemVO> toNormalOrderItemVOList(List<OrderItemDTO> dtoList) {
        if (dtoList == null) return null;
        return dtoList.stream().map(OrderApiConverter::toNormalOrderItemVO).collect(Collectors.toList());
    }

    /**
     * 将 {@link OrderItemDTO} 反向转换为 {@link NormalOrderItemVO}。
     *
     * @param dto 订单明细 DTO
     * @return 订单明细 VO
     */
    public static NormalOrderItemVO toNormalOrderItemVO(OrderItemDTO dto) {
        if (dto == null) return null;
        NormalOrderItemVO vo = new NormalOrderItemVO();
        vo.setId(dto.getId());
        vo.setOrderId(dto.getOrderId());
        vo.setProductId(dto.getProductId());
        vo.setSkuId(dto.getSkuId());
        vo.setSkuAttributes(dto.getSkuAttributes());
        vo.setProductName(dto.getProductName());
        vo.setProductImage(dto.getProductImage());
        vo.setUnitPrice(dto.getUnitPrice());
        vo.setQuantity(dto.getQuantity());
        vo.setSubtotal(dto.getSubtotal());
        return vo;
    }

    /**
     * 将 {@link OrderListItemDTO} 反向转换为 {@link OrderListItemVO}。
     *
     * @param dto 订单列表项 DTO
     * @return 订单列表项 VO
     */
    public static OrderListItemVO toOrderListItemVO(OrderListItemDTO dto) {
        if (dto == null) return null;
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(dto.getId());
        vo.setOrderNo(dto.getOrderNo());
        vo.setOrderType(dto.getOrderType());
        vo.setStatus(dto.getStatus());
        vo.setTotalAmount(dto.getTotalAmount());
        vo.setPayMethod(dto.getPayMethod());
        vo.setCreateTime(dto.getCreateTime());
        vo.setPayTime(dto.getPayTime());
        vo.setShipTime(dto.getShipTime());
        if (dto.getItems() != null) {
            vo.setItems(dto.getItems().stream().map(OrderApiConverter::toOrderItemSnapshot).collect(Collectors.toList()));
        }
        return vo;
    }

    /**
     * 将 {@link OrderItemSnapshotDTO} 反向转换为 {@link OrderListItemVO.OrderItemSnapshot}。
     *
     * @param dto 商品快照 DTO
     * @return 商品快照 VO
     */
    public static OrderListItemVO.OrderItemSnapshot toOrderItemSnapshot(OrderItemSnapshotDTO dto) {
        if (dto == null) return null;
        OrderListItemVO.OrderItemSnapshot snap = new OrderListItemVO.OrderItemSnapshot();
        snap.setProductId(dto.getProductId());
        snap.setProductName(dto.getProductName());
        snap.setProductImage(dto.getProductImage());
        snap.setUnitPrice(dto.getUnitPrice());
        snap.setQuantity(dto.getQuantity());
        return snap;
    }
}