package com.seckill.mall.order.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单列表查询条件（统一查询秒杀订单+普通订单）。
 *
 * <p>业务语义：统一查询秒杀订单+普通订单，合并后按 createTime 降序，内存分页。
 *
 * <p>用户身份由 {@code CurrentUserContext} 注入，不作为 Query 字段。
 *
 * <p>原方法：{@code OrderQueryService.getUnifiedOrderList(userId, status, orderType, pageNum, pageSize)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListQuery {

    /** 订单状态筛选（null=不筛选） */
    private String status;

    /** 订单类型筛选（null/NORMAL/SECKILL） */
    private String orderType;

    /** 页码（默认 1） */
    private Integer pageNum;

    /** 每页大小（默认 10，上限 50） */
    private Integer pageSize;
}