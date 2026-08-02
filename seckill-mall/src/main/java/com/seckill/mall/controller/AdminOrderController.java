package com.seckill.mall.controller;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.AdminOrderQueryRequest;
import com.seckill.mall.service.AdminOrderService;
import com.seckill.mall.vo.AdminOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final AdminOrderService adminOrderService;

    @Operation(summary = "后台订单列表（高级筛选+分页+排序）")
    @GetMapping
    public Result<PageResult<AdminOrderVO>> list(AdminOrderQueryRequest req) {
        return Result.success(adminOrderService.getAdminOrderList(req));
    }
}