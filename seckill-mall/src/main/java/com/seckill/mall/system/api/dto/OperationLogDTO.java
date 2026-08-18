package com.seckill.mall.system.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志 DTO - 替代 OperationLogVO 作为 Application 层返回值。
 *
 * <p>来源映射：{@code OperationLogVO} → {@code OperationLogDTO}（由 SystemApiConverter.toDTO 转换）。
 *
 * <p>详见 SYSTEM-API-CONTRACT.md §2.1。
 *
 * @author wnj
 * @since Phase SY.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogDTO {

    /** 日志 ID */
    private Long id;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作人名称（联表查询） */
    private String operatorName;

    /** 模块名 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 目标 ID */
    private String targetId;

    /** 目标类型 */
    private String targetType;

    /** 详情 */
    private String detail;

    /** IP 地址 */
    private String ipAddress;

    /** 操作时间 */
    private LocalDateTime operationTime;
}