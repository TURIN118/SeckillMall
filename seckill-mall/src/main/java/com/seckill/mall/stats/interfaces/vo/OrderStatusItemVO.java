package com.seckill.mall.stats.interfaces.vo;

import lombok.Data;

/**
 * 订单状态分布项 VO（数据看台专用）
 *
 * <p>用于 {@code /api/v1/admin/stats/order-status-distribution} 接口，
 * 返回每个订单状态对应的订单数量。与 {@link OrderStatusDistributionVO}
 * （含 items 列表与 total 的复合结构，供 SystemController 使用）区分开，
 * 本类仅包含 status 与 count 两个扁平字段，便于前端饼图直接消费。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderStatusItemVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OrderStatusItemVO {

    /**
     * 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED
     */
    private String status;

    /**
     * 该状态订单数量
     */
    private Long count;
}