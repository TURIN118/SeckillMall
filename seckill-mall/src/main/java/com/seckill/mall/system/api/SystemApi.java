package com.seckill.mall.system.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.system.api.dto.OperationLogDTO;
import com.seckill.mall.system.api.dto.OrderStatusDistributionDTO;
import com.seckill.mall.system.api.dto.OrderTrendDTO;
import com.seckill.mall.system.api.dto.SystemHealthDTO;
import com.seckill.mall.system.api.query.OperationLogQuery;

import java.util.List;

/**
 * System 模块 Public API - 后台系统运维能力。
 *
 * <p>提供操作日志管理、系统健康检查、订单趋势与状态分布等运维聚合能力，
 * 供 system 模块 Controller 调用。
 *
 * <p>实现类：{@code com.seckill.mall.system.application.SystemApplicationService}（Phase SY.4 创建）。
 *
 * <p>契约一经发布只增不改不删，DTO 字段只增不删不改变类型。详见 SYSTEM-API-CONTRACT.md。
 *
 * @author wnj
 * @since Phase SY.2
 */
public interface SystemApi {

    /**
     * 操作日志分页查询（按模块/操作人筛选）。
     *
     * @param query 查询条件（pageNum/pageSize/module/operatorId）
     * @return 操作日志分页结果
     */
    PageResult<OperationLogDTO> getOperationLogs(OperationLogQuery query);

    /**
     * 查询所有符合条件的操作日志（不分页），用于 Excel 导出。
     *
     * <p>为防止数据量过大，最多返回 10000 条（按时间倒序）。</p>
     *
     * @param module 模块名筛选，null/空串表示不筛选
     * @return 操作日志列表
     */
    List<OperationLogDTO> listAllForExport(String module);

    /**
     * 系统健康检查：Redis/DB/MQ 状态 + 资源监控 + JVM + DB Pool + 系统信息。
     *
     * @return 系统健康 DTO
     */
    SystemHealthDTO getSystemHealth();

    /**
     * 近 N 天订单趋势（按日期分组统计订单数与销售额，仅统计 PAID/COMPLETED）。
     *
     * @param days 天数，可选，默认 7，最大 30
     * @return 订单趋势 DTO
     */
    OrderTrendDTO getOrderTrend(Integer days);

    /**
     * 订单状态分布（按 status 分组统计）。
     *
     * <p>按 {@code OrderStatus} 枚举自然顺序输出，即使 count=0 也返回，
     * 便于前端饼图直接消费。</p>
     *
     * @param startTime 起始时间字符串（yyyy-MM-dd HH:mm:ss），可选，默认近 30 天
     * @param endTime   结束时间字符串（yyyy-MM-dd HH:mm:ss），可选，默认当前
     * @return 状态分布 DTO
     */
    OrderStatusDistributionDTO getOrderStatusDistribution(String startTime, String endTime);
}