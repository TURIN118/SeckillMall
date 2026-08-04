package com.seckill.mall.dto;

import lombok.Data;

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
    private Integer pageNum;

    /**
     * 每页大小，默认 10
     */
    private Integer pageSize;

    /**
     * 订单号模糊匹配
     */
    private String orderNo;

    /**
     * 按天筛选订单创建日期，格式 yyyy-MM-dd
     * 前端 dateSingle 传入，Mapper 中使用 DATE(create_time) = #{date} 精确匹配当天
     */
    private String date;

    /**
     * 订单状态：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED
     */
    private String status;

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
