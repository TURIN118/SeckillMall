package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.vo.DashboardVO;
import com.seckill.mall.vo.OperationLogVO;
import com.seckill.mall.vo.OrderStatusDistributionVO;
import com.seckill.mall.vo.OrderTrendVO;
import com.seckill.mall.vo.SystemHealthVO;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemService.java
 * 邮箱：nj651217@163.com
 */
public interface SystemService {

    DashboardVO getDashboard();

    PageResult<OperationLogVO> getOperationLogs(OperationLogQueryRequest req);

    /**
     * 查询所有符合条件的操作日志（不分页），用于 Excel 导出。
     * <p>为防止数据量过大，最多返回 {@code MAX_EXPORT_SIZE} 条（当前 10000）。</p>
     *
     * @param module 模块名筛选，null/空串表示不筛选
     * @return 操作日志列表（按时间倒序）
     */
    List<OperationLogVO> listAllForExport(String module);

    SystemHealthVO getSystemHealth();

    /**
     * 近 N 天订单趋势（按日期分组统计订单数与销售额，仅统计 PAID/COMPLETED）
     *
     * @param days 天数，可选，默认 7，最大 30
     * @return 订单趋势
     */
    OrderTrendVO getOrderTrend(Integer days);

    /**
     * 订单状态分布（按 status 分组统计）
     *
     * @param startTime 起始时间字符串（yyyy-MM-dd HH:mm:ss），可选，默认近 30 天
     * @param endTime   结束时间字符串（yyyy-MM-dd HH:mm:ss），可选，默认当前
     * @return 状态分布
     */
    OrderStatusDistributionVO getOrderStatusDistribution(String startTime, String endTime);
}
