package com.seckill.mall.order.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 管理员订单查询条件（后台管理用）。
 *
 * <p>支持多维度筛选：订单号、日期、状态、类型、用户、商品、秒杀活动、金额范围、支付时间、排序等。
 *
 * <p>注意：{@code userId} 字段在此为管理员筛选条件（筛选特定用户的订单），
 * 非当前登录用户身份。当前管理员身份由 {@code CurrentUserContext} 注入。
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderQuery {

    /** 页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 订单号模糊匹配 */
    private String orderNo;

    /** 日期筛选（yyyy-MM-dd） */
    private String date;

    /** 订单状态 */
    private String status;

    /** 订单类型（NORMAL/SECKILL） */
    private String orderType;

    /** 用户 ID（管理员筛选特定用户的订单，非当前登录身份） */
    private Long userId;

    /** 商品 ID */
    private Long productId;

    /** 秒杀活动 ID */
    private Long seckillId;

    /** 创建时间起始 */
    private String startTime;

    /** 创建时间结束 */
    private String endTime;

    /** 创建日期起始 */
    private String startDate;

    /** 创建日期结束 */
    private String endDate;

    /** 最小金额 */
    private BigDecimal minAmount;

    /** 最大金额 */
    private BigDecimal maxAmount;

    /** 支付时间起始 */
    private String payStartTime;

    /** 支付时间结束 */
    private String payEndTime;

    /** 排序字段 */
    private String sortBy;

    /** 排序方向 */
    private String sortOrder;
}