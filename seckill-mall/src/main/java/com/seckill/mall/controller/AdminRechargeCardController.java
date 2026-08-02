package com.seckill.mall.controller;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.RechargeCardGenerateRequest;
import com.seckill.mall.service.RechargeCardService;
import com.seckill.mall.vo.RechargeCardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 充值卡后台管理 Controller
 * <p>
 * 前缀 {@code /api/v1/admin/recharge-cards}，需 ADMIN 角色。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminRechargeCardController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "充值卡后台管理", description = "批量生成/列表/禁用")
@RestController
@RequestMapping("/api/v1/admin/recharge-cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRechargeCardController {

    private final RechargeCardService rechargeCardService;

    @Operation(summary = "查询充值卡列表（分页，可按批次号筛选）")
    @GetMapping("/list")
    public Result<PageResult<RechargeCardVO>> list(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                                   @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String batchNo,
                                                   @RequestParam(required = false) String status) {
        return Result.success(rechargeCardService.listPage(pageNum, pageSize, batchNo, status));
    }

    @Operation(summary = "批量生成充值卡（返回明文卡密，仅此一次）")
    @PostMapping("/generate")
    public Result<List<RechargeCardVO>> generate(@Valid @RequestBody RechargeCardGenerateRequest req) {
        return Result.success("生成成功", rechargeCardService.generate(req.getFaceValue(), req.getCount()));
    }

    @Operation(summary = "禁用充值卡")
    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        rechargeCardService.disable(id);
        return Result.<Void>success("禁用成功", null);
    }
}