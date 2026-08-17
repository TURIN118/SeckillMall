package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单详情 DTO。
 *
 * <p>由 {@code getOrderDetail}、{@code payOrder}、{@code cancelOrder} 等方法返回，
 * 包含订单基础信息、明细列表与收货地址信息。替代 {@code NormalOrderDetailVO} 用于模块间通信。
 *
 * <p>属于 API 层数据契约，与 infrastructure 层、interfaces 层 VO 隔离。
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDTO {

    /** 订单基础信息 */
    private OrderDTO order;

    /** 订单明细列表 */
    private List<OrderItemDTO> items;

    /** 收件人 */
    private String receiverName;

    /** 手机号 */
    private String receiverPhone;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 详细地址 */
    private String detailAddress;
}