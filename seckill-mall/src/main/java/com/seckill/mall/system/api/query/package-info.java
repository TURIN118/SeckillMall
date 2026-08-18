/**
 * System API 查询对象（读操作入参）。
 *
 * <p>包含操作日志查询条件（OperationLogQuery），封装分页与筛选参数，
 * 替代 HTTP 请求对象 OperationLogQueryRequest 作为 API 层入参。
 *
 * <p>Query 对象不带 validation 注解，validation 责责留在 interfaces/web 层的 Request 对象。
 *
 * @author wnj
 * @since Phase SY.0
 */
package com.seckill.mall.system.api.query;