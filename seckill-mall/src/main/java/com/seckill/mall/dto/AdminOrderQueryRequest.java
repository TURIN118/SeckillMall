package com.seckill.mall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminOrderQueryRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class AdminOrderQueryRequest {

    /**
     * 页码，默认 1
     */
    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer pageNum;

    /**
     * 每页大小，默认 10
     */
    @Min(value = 1, message = "每页大小必须大于等于 1")
    @Max(value = 100, message = "每页大小不能超过 100")
    private Integer pageSize;

    /**
     * 订单号模糊匹配
     */
    private String orderNo;

    /**
     * 按天筛选订单创建日期，格式 yyyy-MM-dd
     * 前端 dateSingle 传入，Mapper 中使用 DATE(create_time) = #{date} 精确匹配当天
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String date;

    /**
     * 订单状态：UNPAID/PAID/SHIPPED/CANCELLED/TIMEOUT/COMPLETED
     */
    @Pattern(regexp = "^(UNPAID|PAID|SHIPPED|CANCELLED|TIMEOUT|COMPLETED)$", message = "订单状态非法")
    private String status;

    /**
     * 订单类型筛选：NORMAL-普通订单 / SECKILL-秒杀订单
     * 为空时同时查询普通订单和秒杀订单（任务#49）
     */
    @Pattern(regexp = "^(NORMAL|SECKILL)$", message = "订单类型非法，仅支持 NORMAL 或 SECKILL")
    private String orderType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 秒杀活动ID
     */
    private Long seckillId;

    /**
     * 创建时间起始，格式 yyyy-MM-dd HH:mm:ss
     */
    private String startTime;

    /**
     * 创建时间结束，格式 yyyy-MM-dd HH:mm:ss
     */
    private String endTime;

    /**
     * 创建日期起始，格式 yyyy-MM-dd（任务#54 导出弹窗时间范围起始）
     * Service 层解析为当天 00:00:00 填充 startLdt，与 date 单日筛选互斥（date 优先）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "起始日期格式必须为 yyyy-MM-dd")
    private String startDate;

    /**
     * 创建日期结束，格式 yyyy-MM-dd（任务#54 导出弹窗时间范围结束）
     * Service 层解析为次日 00:00:00 填充 endLdt，与 date 单日筛选互斥（date 优先）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为 yyyy-MM-dd")
    private String endDate;

    /**
     * 最小金额（含），筛选 total_amount &gt;= minAmount（任务#54 导出弹窗金额范围）
     */
    @DecimalMin(value = "0", message = "最小金额不能小于 0")
    private BigDecimal minAmount;

    /**
     * 最大金额（含），筛选 total_amount &lt;= maxAmount（任务#54 导出弹窗金额范围）
     */
    @DecimalMin(value = "0", message = "最大金额不能小于 0")
    private BigDecimal maxAmount;

    /**
     * 支付时间起始，格式 yyyy-MM-dd HH:mm:ss
     */
    private String payStartTime;

    /**
     * 支付时间结束，格式 yyyy-MM-dd HH:mm:ss
     */
    private String payEndTime;

    /**
     * 排序字段：createTime/payTime/totalAmount，默认 createTime
     */
    private String sortBy;

    /**
     * 排序方向：asc/desc，默认 desc
     */
    private String sortOrder;

    // ====== 以下字段为 Service 层归一化后内部传递使用，不参与请求绑定 ======

    /**
     * 创建时间起始（解析后）
     */
    private LocalDateTime startLdt;

    /**
     * 创建时间结束（解析后）
     */
    private LocalDateTime endLdt;

    /**
     * 支付时间起始（解析后）
     */
    private LocalDateTime payStartLdt;

    /**
     * 支付时间结束（解析后）
     */
    private LocalDateTime payEndLdt;

    /**
     * 白名单归一化后的排序列名（create_time/pay_time/total_amount）
     */
    private String sortByColumn;
}
