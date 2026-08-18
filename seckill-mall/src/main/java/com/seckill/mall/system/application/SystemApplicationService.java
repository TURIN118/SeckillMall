package com.seckill.mall.system.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.service.SystemService;
import com.seckill.mall.system.api.SystemApi;
import com.seckill.mall.system.api.dto.OperationLogDTO;
import com.seckill.mall.system.api.dto.OrderStatusDistributionDTO;
import com.seckill.mall.system.api.dto.OrderTrendDTO;
import com.seckill.mall.system.api.dto.SystemHealthDTO;
import com.seckill.mall.system.api.query.OperationLogQuery;
import com.seckill.mall.system.application.facade.SystemApiConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * System 应用层服务 - Strangler Pattern 门面阶段。
 *
 * <p>实现 {@link SystemApi}，内部委托旧 {@link SystemService}（保持原业务实现不动），
 * 通过 {@link SystemApiConverter} 在 VO ↔ DTO 之间做字段映射。
 *
 * <p>迁移阶段：Phase SY.4（Strangler Pattern 过渡期）。
 * <ul>
 *   <li>旧 SystemService/SystemServiceImpl 保持原位不动，业务逻辑零修改</li>
 *   <li>本类仅做"接口适配 + 类型转换"，不重写任何业务流程</li>
 *   <li>Controller 切换调用 SystemApi 后，旧 SystemService 仍可被其他处引用（向后兼容）</li>
 * </ul>
 *
 * <p>后续 Phase SY.5+ 将逐步把 SystemServiceImpl 的业务实现迁入 application/infrastructure，
 * 并最终删除旧 SystemService。
 *
 * @author wnj
 * @since Phase SY.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemApplicationService implements SystemApi {

    private final SystemService systemService;

    @Override
    public PageResult<OperationLogDTO> getOperationLogs(OperationLogQuery query) {
        // 适配层：API Query → 旧 Request 对象，复用旧 SystemService 实现
        OperationLogQueryRequest req = toRequest(query);
        PageResult<com.seckill.mall.system.interfaces.vo.OperationLogVO> voPage =
                systemService.getOperationLogs(req);
        return SystemApiConverter.toDTOPage(voPage);
    }

    @Override
    public List<OperationLogDTO> listAllForExport(String module) {
        List<com.seckill.mall.system.interfaces.vo.OperationLogVO> voList =
                systemService.listAllForExport(module);
        return SystemApiConverter.toDTOList(voList);
    }

    @Override
    public SystemHealthDTO getSystemHealth() {
        com.seckill.mall.system.interfaces.vo.SystemHealthVO vo = systemService.getSystemHealth();
        return SystemApiConverter.toDTO(vo);
    }

    @Override
    public OrderTrendDTO getOrderTrend(Integer days) {
        com.seckill.mall.system.interfaces.vo.OrderTrendVO vo = systemService.getOrderTrend(days);
        return SystemApiConverter.toDTO(vo);
    }

    @Override
    public OrderStatusDistributionDTO getOrderStatusDistribution(String startTime, String endTime) {
        com.seckill.mall.system.interfaces.vo.OrderStatusDistributionVO vo =
                systemService.getOrderStatusDistribution(startTime, endTime);
        return SystemApiConverter.toDTO(vo);
    }

    /**
     * API 层 Query → 旧 Service 层 Request 适配。
     *
     * <p>不携带 validation 注解（validation 已在 Controller Request 对象完成），
     * 此处仅做字段拷贝，保持默认值与旧 Request 一致。
     */
    private static OperationLogQueryRequest toRequest(OperationLogQuery query) {
        OperationLogQueryRequest req = new OperationLogQueryRequest();
        if (query == null) {
            return req;
        }
        req.setPageNum(query.getPageNum());
        req.setPageSize(query.getPageSize());
        req.setModule(query.getModule());
        req.setOperatorId(query.getOperatorId());
        return req;
    }
}