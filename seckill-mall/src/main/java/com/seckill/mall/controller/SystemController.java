package com.seckill.mall.controller;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.service.SystemService;
import com.seckill.mall.vo.DashboardVO;
import com.seckill.mall.vo.OperationLogVO;
import com.seckill.mall.vo.OrderStatusDistributionVO;
import com.seckill.mall.vo.OrderTrendVO;
import com.seckill.mall.vo.SystemHealthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "系统管理", description = "仪表盘/日志/健康检查")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemController {

    private final SystemService systemService;

    @Operation(summary = "仪表盘统计")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(systemService.getDashboard());
    }

    @Operation(summary = "操作日志列表（分页+模块筛选）")
    @GetMapping("/operation-logs")
    public Result<PageResult<OperationLogVO>> operationLogs(OperationLogQueryRequest req) {
        return Result.success(systemService.getOperationLogs(req));
    }

    @Operation(summary = "系统健康检查")
    @GetMapping("/system/health")
    public Result<SystemHealthVO> systemHealth() {
        return Result.success(systemService.getSystemHealth());
    }

    @Operation(summary = "近N天订单趋势（按日期分组统计订单数与销售额）")
    @GetMapping("/dashboard/order-trend")
    public Result<OrderTrendVO> orderTrend(@RequestParam(required = false) Integer days) {
        return Result.success(systemService.getOrderTrend(days));
    }

    @Operation(summary = "订单状态分布（按状态分组统计数量与占比）")
    @GetMapping("/dashboard/order-status-distribution")
    public Result<OrderStatusDistributionVO> orderStatusDistribution(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(systemService.getOrderStatusDistribution(startTime, endTime));
    }
}
