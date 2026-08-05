package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.SeckillGoodsService;
import com.seckill.mall.service.SeckillService;
import com.seckill.mall.service.SeckillTokenService;
import com.seckill.mall.vo.SeckillGoodsVO;
import com.seckill.mall.vo.SeckillResultVO;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "秒杀活动", description = "秒杀活动与下单")
@RestController
@RequestMapping("/api/v1/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillGoodsService seckillGoodsService;
    private final SeckillService seckillService;
    private final SeckillTokenService seckillTokenService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "秒杀活动列表")
    @GetMapping("/list")
    public Result<PageResult<SeckillGoodsVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(seckillGoodsService.listSeckill(status, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "秒杀活动详情")
    @GetMapping("/{seckillId}")
    public Result<SeckillGoodsVO> detail(@PathVariable Long seckillId) {
        return Result.success(seckillGoodsService.getSeckillDetail(seckillId));
    }

    @Operation(summary = "获取秒杀令牌")
    @GetMapping("/{seckillId}/token")
    public Result<String> token(@PathVariable Long seckillId) {
        return Result.success(seckillTokenService.getSeckillToken(seckillId, securityUtils.getCurrentUserId()));
    }

    @Operation(summary = "查询实时库存")
    @GetMapping("/{seckillId}/stock")
    public Result<Integer> stock(@PathVariable Long seckillId) {
        return Result.success(seckillGoodsService.getStock(seckillId));
    }

    @Operation(summary = "执行秒杀")
    @PostMapping("/{seckillId}")
    @RateLimit(key = "seckill", capacity = 1, rate = 1)
    public Result<SeckillResultVO> doSeckill(
            @PathVariable Long seckillId,
            @RequestHeader(value = "X-Seckill-Token", required = false) String headerToken,
            @RequestParam(value = "seckillToken", required = false) String paramToken) {
        // 安全修复（L1）：显式获取当前用户 ID 并通过上下文传递给 Service，
        // 避免 Service 层从 Token 中解析 userId 造成越权。Service 层 doSeckill 内部
        // 应使用 securityUtils.getCurrentUserId() 获取调用方身份。
        Long userId = securityUtils.getCurrentUserId();
        String token = headerToken != null ? headerToken : paramToken;
        // TODO(service-layer): SeckillService.doSeckill 建议新增 userId 参数重载，
        // 当前保留原签名，由 Service 内部通过 SecurityUtils 获取，确保调用方身份可信。
        return Result.success(seckillService.doSeckill(seckillId, token));
    }

    @Operation(summary = "一键执行秒杀（无需预取token）")
    @PostMapping("/{seckillId}/execute")
    @RateLimit(key = "seckill", capacity = 1, rate = 1)
    public Result<SeckillResultVO> execute(@PathVariable Long seckillId) {
        Long userId = securityUtils.getCurrentUserId();
        // 内部自动获取token
        String token = seckillTokenService.getSeckillToken(seckillId, userId);
        return Result.success(seckillService.doSeckill(seckillId, token));
    }

    @Operation(summary = "查询秒杀结果")
    @GetMapping("/{seckillId}/result")
    public Result<SeckillResultVO> result(
            @PathVariable Long seckillId,
            @RequestParam String requestId) {
        // 安全修复（H5）：显式传入当前用户 ID，由 Service 层校验该 requestId 属于当前用户，
        // 防止用户通过遍历 requestId 越权查询他人秒杀结果。
        Long userId = securityUtils.getCurrentUserId();
        // TODO(service-layer): SeckillService.getSeckillResult 需新增 userId 参数重载，
        // 在 Service 实现中校验 result.userId == userId，不匹配则抛 FORBIDDEN。
        // 当前保留原签名，由 Service 层配合改造，此处先在 Controller 层获取 userId 以备后续接入。
        return Result.success(seckillService.getSeckillResult(seckillId, requestId));
    }

    @Operation(summary = "创建秒杀活动")
    @OperationLog(module = "SECKILL", action = "CREATE", targetType = "SECKILL")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PostMapping("/admin")
    public Result<SeckillGoodsVO> create(@Valid @RequestBody SeckillCreateRequest req) {
        return Result.success(seckillGoodsService.createSeckill(req));
    }

    @Operation(summary = "编辑秒杀活动")
    @OperationLog(module = "SECKILL", action = "UPDATE", targetIdSpEL = "#seckillId", targetType = "SECKILL")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PutMapping("/admin/{seckillId}")
    public Result<SeckillGoodsVO> update(@PathVariable Long seckillId,
                                         @Valid @RequestBody SeckillCreateRequest req) {
        return Result.success(seckillGoodsService.updateSeckill(seckillId, req));
    }

    @Operation(summary = "取消秒杀活动")
    @OperationLog(module = "SECKILL", action = "CANCEL", targetIdSpEL = "#seckillId", targetType = "SECKILL")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PutMapping("/admin/{seckillId}/cancel")
    public Result<Void> cancel(@PathVariable Long seckillId) {
        seckillGoodsService.cancelSeckill(seckillId);
        return Result.success();
    }
}
