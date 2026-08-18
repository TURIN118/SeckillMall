package com.seckill.mall.system.application.facade;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.system.api.dto.OperationLogDTO;
import com.seckill.mall.system.api.dto.OrderStatusDistributionDTO;
import com.seckill.mall.system.api.dto.OrderTrendDTO;
import com.seckill.mall.system.api.dto.SystemHealthDTO;
import com.seckill.mall.system.interfaces.vo.OperationLogVO;
import com.seckill.mall.system.interfaces.vo.OrderStatusDistributionVO;
import com.seckill.mall.system.interfaces.vo.OrderTrendVO;
import com.seckill.mall.system.interfaces.vo.SystemHealthVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * System 应用层转换器 - VO ↔ DTO 双向映射。
 *
 * <p>无状态静态工具类，集中处理旧 VO（{@code com.seckill.mall.system.interfaces.vo}）
 * 与新 API 层 DTO（{@code com.seckill.mall.system.api.dto}）之间的字段映射。
 *
 * <p>设计约束：
 * <ul>
 *   <li>禁止修改任何业务字段或类型，仅做 1:1 字段拷贝</li>
 *   <li>所有方法对 null 入参返回 null（保持与原 SystemService 行为一致）</li>
 *   <li>列表/分页转换保持原顺序</li>
 * </ul>
 *
 * <p>详见 SYSTEM-API-CONTRACT.md §4。
 *
 * @author wnj
 * @since Phase SY.4
 */
public final class SystemApiConverter {

    private SystemApiConverter() {
        // 工具类，禁止实例化
    }

    // ==================== OperationLog ====================

    /** OperationLogVO → OperationLogDTO */
    public static OperationLogDTO toDTO(OperationLogVO vo) {
        if (vo == null) {
            return null;
        }
        return OperationLogDTO.builder()
                .id(vo.getId())
                .operatorId(vo.getOperatorId())
                .operatorName(vo.getOperatorName())
                .module(vo.getModule())
                .action(vo.getAction())
                .targetId(vo.getTargetId())
                .targetType(vo.getTargetType())
                .detail(vo.getDetail())
                .ipAddress(vo.getIpAddress())
                .operationTime(vo.getOperationTime())
                .build();
    }

    /** OperationLogDTO → OperationLogVO */
    public static OperationLogVO toVO(OperationLogDTO dto) {
        if (dto == null) {
            return null;
        }
        OperationLogVO vo = new OperationLogVO();
        vo.setId(dto.getId());
        vo.setOperatorId(dto.getOperatorId());
        vo.setOperatorName(dto.getOperatorName());
        vo.setModule(dto.getModule());
        vo.setAction(dto.getAction());
        vo.setTargetId(dto.getTargetId());
        vo.setTargetType(dto.getTargetType());
        vo.setDetail(dto.getDetail());
        vo.setIpAddress(dto.getIpAddress());
        vo.setOperationTime(dto.getOperationTime());
        return vo;
    }

    /** List&lt;OperationLogVO&gt; → List&lt;OperationLogDTO&gt; */
    public static List<OperationLogDTO> toDTOList(List<OperationLogVO> voList) {
        if (voList == null) {
            return null;
        }
        if (voList.isEmpty()) {
            return Collections.emptyList();
        }
        List<OperationLogDTO> dtoList = new ArrayList<>(voList.size());
        for (OperationLogVO vo : voList) {
            dtoList.add(toDTO(vo));
        }
        return dtoList;
    }

    /** List&lt;OperationLogDTO&gt; → List&lt;OperationLogVO&gt; */
    public static List<OperationLogVO> toVOList(List<OperationLogDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        if (dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        List<OperationLogVO> voList = new ArrayList<>(dtoList.size());
        for (OperationLogDTO dto : dtoList) {
            voList.add(toVO(dto));
        }
        return voList;
    }

    /** PageResult&lt;OperationLogVO&gt; → PageResult&lt;OperationLogDTO&gt; */
    public static PageResult<OperationLogDTO> toDTOPage(PageResult<OperationLogVO> voPage) {
        if (voPage == null) {
            return null;
        }
        List<OperationLogDTO> dtoList = toDTOList(voPage.getList());
        return new PageResult<>(
                dtoList,
                voPage.getTotal(),
                voPage.getPageNum(),
                voPage.getPageSize(),
                voPage.getPages());
    }

    /** PageResult&lt;OperationLogDTO&gt; → PageResult&lt;OperationLogVO&gt; */
    public static PageResult<OperationLogVO> toVOPage(PageResult<OperationLogDTO> dtoPage) {
        if (dtoPage == null) {
            return null;
        }
        List<OperationLogVO> voList = toVOList(dtoPage.getList());
        return new PageResult<>(
                voList,
                dtoPage.getTotal(),
                dtoPage.getPageNum(),
                dtoPage.getPageSize(),
                dtoPage.getPages());
    }

    // ==================== SystemHealth ====================

    /** SystemHealthVO → SystemHealthDTO */
    public static SystemHealthDTO toDTO(SystemHealthVO vo) {
        if (vo == null) {
            return null;
        }
        return SystemHealthDTO.builder()
                .redis(vo.getRedis())
                .database(vo.getDatabase())
                .mq(vo.getMq())
                .cpuUsage(vo.getCpuUsage())
                .memoryUsage(vo.getMemoryUsage())
                .diskUsage(vo.getDiskUsage())
                .redisHitRate(vo.getRedisHitRate())
                .redisResponseTime(vo.getRedisResponseTime())
                .dbPoolUsage(vo.getDbPoolUsage())
                .mqQueueBacklog(vo.getMqQueueBacklog())
                .jvmHeapUsage(vo.getJvmHeapUsage())
                .jvmNonHeapUsage(vo.getJvmNonHeapUsage())
                .dbActiveConnections(vo.getDbActiveConnections())
                .dbIdleConnections(vo.getDbIdleConnections())
                .dbMaxConnections(vo.getDbMaxConnections())
                .osName(vo.getOsName())
                .jdkVersion(vo.getJdkVersion())
                .appStartTime(vo.getAppStartTime())
                .appUptime(vo.getAppUptime())
                .build();
    }

    /** SystemHealthDTO → SystemHealthVO */
    public static SystemHealthVO toVO(SystemHealthDTO dto) {
        if (dto == null) {
            return null;
        }
        SystemHealthVO vo = new SystemHealthVO();
        vo.setRedis(dto.getRedis());
        vo.setDatabase(dto.getDatabase());
        vo.setMq(dto.getMq());
        vo.setCpuUsage(dto.getCpuUsage());
        vo.setMemoryUsage(dto.getMemoryUsage());
        vo.setDiskUsage(dto.getDiskUsage());
        vo.setRedisHitRate(dto.getRedisHitRate());
        vo.setRedisResponseTime(dto.getRedisResponseTime());
        vo.setDbPoolUsage(dto.getDbPoolUsage());
        vo.setMqQueueBacklog(dto.getMqQueueBacklog());
        vo.setJvmHeapUsage(dto.getJvmHeapUsage());
        vo.setJvmNonHeapUsage(dto.getJvmNonHeapUsage());
        vo.setDbActiveConnections(dto.getDbActiveConnections());
        vo.setDbIdleConnections(dto.getDbIdleConnections());
        vo.setDbMaxConnections(dto.getDbMaxConnections());
        vo.setOsName(dto.getOsName());
        vo.setJdkVersion(dto.getJdkVersion());
        vo.setAppStartTime(dto.getAppStartTime());
        vo.setAppUptime(dto.getAppUptime());
        return vo;
    }

    // ==================== OrderTrend ====================

    /** OrderTrendVO → OrderTrendDTO */
    public static OrderTrendDTO toDTO(OrderTrendVO vo) {
        if (vo == null) {
            return null;
        }
        return OrderTrendDTO.builder()
                .dates(vo.getDates())
                .orderCounts(vo.getOrderCounts())
                .salesAmounts(vo.getSalesAmounts())
                .build();
    }

    /** OrderTrendDTO → OrderTrendVO */
    public static OrderTrendVO toVO(OrderTrendDTO dto) {
        if (dto == null) {
            return null;
        }
        OrderTrendVO vo = new OrderTrendVO();
        vo.setDates(dto.getDates());
        vo.setOrderCounts(dto.getOrderCounts());
        vo.setSalesAmounts(dto.getSalesAmounts());
        return vo;
    }

    // ==================== OrderStatusDistribution ====================

    /** OrderStatusDistributionVO → OrderStatusDistributionDTO */
    public static OrderStatusDistributionDTO toDTO(OrderStatusDistributionVO vo) {
        if (vo == null) {
            return null;
        }
        List<OrderStatusDistributionDTO.StatusItem> dtoItems = null;
        if (vo.getItems() != null) {
            dtoItems = new ArrayList<>(vo.getItems().size());
            for (OrderStatusDistributionVO.StatusItem src : vo.getItems()) {
                dtoItems.add(OrderStatusDistributionDTO.StatusItem.builder()
                        .status(src.getStatus())
                        .count(src.getCount())
                        .percentage(src.getPercentage())
                        .build());
            }
        }
        return OrderStatusDistributionDTO.builder()
                .items(dtoItems)
                .total(vo.getTotal())
                .build();
    }

    /** OrderStatusDistributionDTO → OrderStatusDistributionVO */
    public static OrderStatusDistributionVO toVO(OrderStatusDistributionDTO dto) {
        if (dto == null) {
            return null;
        }
        OrderStatusDistributionVO vo = new OrderStatusDistributionVO();
        if (dto.getItems() != null) {
            List<OrderStatusDistributionVO.StatusItem> voItems = new ArrayList<>(dto.getItems().size());
            for (OrderStatusDistributionDTO.StatusItem src : dto.getItems()) {
                OrderStatusDistributionVO.StatusItem item = new OrderStatusDistributionVO.StatusItem();
                item.setStatus(src.getStatus());
                item.setCount(src.getCount());
                item.setPercentage(src.getPercentage());
                voItems.add(item);
            }
            vo.setItems(voItems);
        }
        vo.setTotal(dto.getTotal());
        return vo;
    }
}