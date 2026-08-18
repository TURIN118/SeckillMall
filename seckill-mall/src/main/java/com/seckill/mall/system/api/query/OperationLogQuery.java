package com.seckill.mall.system.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 操作日志查询条件 - 封装 SystemApi.getOperationLogs 入参。
 *
 * <p>替代 HTTP 请求对象 {@code OperationLogQueryRequest} 作为 API 层入参。
 * Query 对象不带 validation 注解，validation 责责留在 interfaces/web 层的 Request 对象。
 *
 * <p>字段一一对应 {@code OperationLogQueryRequest}，由 Controller 在适配层转换：Request → Query。
 *
 * <p>详见 SYSTEM-API-CONTRACT.md §3.1。
 *
 * @author wnj
 * @since Phase SY.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogQuery {

    /** 页码（默认 1） */
    private Integer pageNum;

    /** 每页大小（默认 10，上限 100） */
    private Integer pageSize;

    /** 模块名筛选（null=不筛选） */
    private String module;

    /** 操作人 ID 筛选（null=不筛选） */
    private Long operatorId;
}