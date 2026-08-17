/**
 * Order 应用服务 - 订单写操作编排（实现 OrderApi）。
 *
 * <p>Phase 3.4-A Strangler Pattern：委托旧 OrderService/OrderLifecycleService/OrderQueryService，
 * 内部做 Command→旧参数 与 VO→Result/DTO 转换，不重写业务逻辑。
 *
 * @author wnj
 * @since Phase 3.4-A
 */
package com.seckill.mall.order.application.command;