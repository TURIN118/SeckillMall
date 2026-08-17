package com.seckill.mall.order.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.order.api.dto.AdminOrderDTO;
import com.seckill.mall.order.api.query.AdminOrderQuery;

/**
 * Order 模块后台管理能力 API。
 *
 * <p>对外暴露后台管理契约，包括管理员订单列表查询（多维度筛选）。
 * 发货等状态流转操作由 {@link OrderApi} 提供（业务语义统一）。
 *
 * <p>设计原则：
 * <ul>
 *     <li>入参用 Query 对象，支持多维度筛选</li>
 *     <li>出参用 AdminOrderDTO，包含管理员视角的订单信息</li>
 *     <li>当前管理员身份由 {@code CurrentUserContext} 注入，不作为方法参数</li>
 * </ul>
 *
 * @author wnj
 * @since Phase 3.2
 */
public interface AdminOrderApi {

    /**
     * 管理员订单列表查询（多维度筛选）。
     *
     * <p>支持按订单号、日期、状态、类型、用户、商品、秒杀活动、金额范围、支付时间、排序等维度筛选。
     * 统一查询秒杀订单与普通订单。
     *
     * @param query 管理员订单查询条件
     * @return 分页结果
     */
    PageResult<AdminOrderDTO> adminListOrders(AdminOrderQuery query);
}