/**
 * Order 应用服务 - 订单查询编排（实现 OrderQueryApi）。
 *
 * <p>Phase 3.4-A Strangler Pattern：委托旧 OrderQueryService/OrderService，
 * 内部做 Query→旧参数 与 VO→DTO 转换，不重写业务逻辑。
 *
 * @author wnj
 * @since Phase 3.4-A
 */
package com.seckill.mall.order.application.query;