package com.seckill.mall.order.interfaces.web;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.AdminOrderQueryRequest;
import com.seckill.mall.order.api.AdminOrderApi;
import com.seckill.mall.order.api.dto.AdminOrderDTO;
import com.seckill.mall.order.application.facade.OrderApiConverter;
import com.seckill.mall.vo.AdminOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminOrderController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "后台订单管理", description = "后台订单高级筛选")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderApi adminOrderApi;

    @Operation(summary = "后台订单列表（高级筛选+分页+排序）")
    @GetMapping
    public Result<PageResult<AdminOrderVO>> list(@Valid AdminOrderQueryRequest req) {
        PageResult<AdminOrderDTO> dtoPage = adminOrderApi.adminListOrders(
                OrderApiConverter.toAdminOrderQuery(req)
        );
        if (dtoPage == null || dtoPage.getList() == null) {
            return Result.success(new PageResult<>(null, 0, 1, 10, 0));
        }
        List<AdminOrderVO> voList = dtoPage.getList().stream()
                .map(OrderApiConverter::toAdminOrderVO)
                .collect(Collectors.toList());
        PageResult<AdminOrderVO> voPage = new PageResult<>(voList, dtoPage.getTotal(),
                dtoPage.getPageNum(), dtoPage.getPageSize(), dtoPage.getPages());
        return Result.success(voPage);
    }
}
