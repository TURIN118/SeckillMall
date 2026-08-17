package com.seckill.mall.order.application.lifecycle;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.order.api.AdminOrderApi;
import com.seckill.mall.order.api.dto.AdminOrderDTO;
import com.seckill.mall.order.api.query.AdminOrderQuery;
import com.seckill.mall.order.application.facade.OrderApiConverter;
import com.seckill.mall.service.AdminOrderService;
import com.seckill.mall.vo.AdminOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台管理应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link AdminOrderApi}，内部委托给旧 {@link AdminOrderService}，
 * 通过 {@link OrderApiConverter} 做 Query→Request 和 VO→DTO 转换。
 *
 * @author wnj
 * @since Phase 3.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleApplicationService implements AdminOrderApi {

    private final AdminOrderService adminOrderService;

    @Override
    public PageResult<AdminOrderDTO> adminListOrders(AdminOrderQuery query) {
        PageResult<AdminOrderVO> voPage = adminOrderService.getAdminOrderList(
                OrderApiConverter.toAdminOrderQueryRequest(query)
        );
        if (voPage == null || voPage.getList() == null) {
            return new PageResult<>(Collections.emptyList(), 0, 1, 10, 0);
        }
        List<AdminOrderDTO> dtoList = voPage.getList().stream()
                .map(OrderApiConverter::toAdminOrderDTO)
                .collect(Collectors.toList());
        return new PageResult<>(dtoList, voPage.getTotal(), voPage.getPageNum(),
                voPage.getPageSize(), voPage.getPages());
    }
}