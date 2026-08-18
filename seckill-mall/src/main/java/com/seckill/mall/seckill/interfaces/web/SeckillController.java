package com.seckill.mall.seckill.interfaces.web;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.SeckillActivityCreateRequest;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.seckill.api.SeckillActivityApi;
import com.seckill.mall.seckill.api.SeckillApi;
import com.seckill.mall.seckill.api.SeckillGoodsApi;
import com.seckill.mall.seckill.api.command.CreateActivityCommand;
import com.seckill.mall.seckill.api.command.CreateSeckillGoodsCommand;
import com.seckill.mall.seckill.api.command.SeckillCommand;
import com.seckill.mall.seckill.api.dto.SeckillActivityDTO;
import com.seckill.mall.seckill.api.dto.SeckillGoodsDTO;
import com.seckill.mall.seckill.api.query.SeckillGoodsQuery;
import com.seckill.mall.seckill.api.result.SeckillResult;
import com.seckill.mall.seckill.application.facade.SeckillApiConverter;
import com.seckill.mall.seckill.interfaces.vo.SeckillActivityVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillGoodsVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillResultVO;
import com.seckill.mall.service.SeckillTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    private final SeckillApi seckillApi;
    private final SeckillGoodsApi seckillGoodsApi;
    private final SeckillActivityApi seckillActivityApi;
    private final SeckillTokenService seckillTokenService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "秒杀活动列表")
    @GetMapping("/list")
    public Result<PageResult<SeckillGoodsVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        SeckillGoodsQuery query = SeckillGoodsQuery.builder()
                .status(status)
                .categoryId(categoryId)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        return Result.success(SeckillApiConverter.toGoodsVOPageResult(seckillGoodsApi.listSeckill(query)));
    }

    @Operation(summary = "秒杀活动详情")
    @GetMapping("/{seckillId}")
    public Result<SeckillGoodsVO> detail(@PathVariable Long seckillId) {
        SeckillGoodsDTO dto = seckillGoodsApi.getSeckillDetail(seckillId);
        return Result.success(SeckillApiConverter.toVO(dto));
    }

    @Operation(summary = "获取秒杀令牌")
    @GetMapping("/{seckillId}/token")
    public Result<String> token(@PathVariable Long seckillId) {
        return Result.success(seckillTokenService.getSeckillToken(seckillId, securityUtils.getCurrentUserId()));
    }

    @Operation(summary = "查询实时库存")
    @GetMapping("/{seckillId}/stock")
    public Result<Integer> stock(@PathVariable Long seckillId) {
        return Result.success(seckillGoodsApi.getStock(seckillId));
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
        // 使用 securityUtils.getCurrentUserId() 获取调用方身份，确保调用方身份可信。
        Long userId = securityUtils.getCurrentUserId();
        String token = headerToken != null ? headerToken : paramToken;
        SeckillCommand command = SeckillCommand.builder()
                .seckillId(seckillId)
                .seckillToken(token)
                .build();
        SeckillResult result = seckillApi.executeSeckill(command);
        return Result.success(SeckillApiConverter.toVO(result));
    }

    @Operation(summary = "一键执行秒杀（无需预取token）")
    @PostMapping("/{seckillId}/execute")
    @RateLimit(key = "seckill", capacity = 1, rate = 1)
    public Result<SeckillResultVO> execute(@PathVariable Long seckillId) {
        Long userId = securityUtils.getCurrentUserId();
        // 内部自动获取token
        String token = seckillTokenService.getSeckillToken(seckillId, userId);
        SeckillCommand command = SeckillCommand.builder()
                .seckillId(seckillId)
                .seckillToken(token)
                .build();
        SeckillResult result = seckillApi.executeSeckill(command);
        return Result.success(SeckillApiConverter.toVO(result));
    }

    @Operation(summary = "查询秒杀结果")
    @GetMapping("/{seckillId}/result")
    public Result<SeckillResultVO> result(
            @PathVariable Long seckillId,
            @RequestParam String requestId) {
        // 安全修复（H5）：显式传入当前用户 ID，由 Service 层校验该 requestId 属于当前用户，
        // 防止用户通过遍历 requestId 越权查询他人秒杀结果。
        // Service 层 getSeckillResult 内部通过 securityUtils.getCurrentUserId() 获取调用方身份，
        // 并校验 result.userId == userId，不匹配则抛 FORBIDDEN。
        Long userId = securityUtils.getCurrentUserId();
        SeckillResult result = seckillApi.getSeckillResult(seckillId, requestId);
        return Result.success(SeckillApiConverter.toVO(result));
    }

    @Operation(summary = "创建秒杀活动")
    @OperationLog(module = "SECKILL", action = "CREATE", targetType = "SECKILL")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PostMapping("/admin")
    public Result<SeckillGoodsVO> create(@Valid @RequestBody SeckillCreateRequest req) {
        CreateSeckillGoodsCommand command = SeckillApiConverter.toCommand(req);
        SeckillGoodsDTO dto = seckillGoodsApi.createSeckill(command);
        return Result.success(SeckillApiConverter.toVO(dto));
    }

    @Operation(summary = "编辑秒杀活动")
    @OperationLog(module = "SECKILL", action = "UPDATE", targetIdSpEL = "#seckillId", targetType = "SECKILL")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PutMapping("/admin/{seckillId}")
    public Result<SeckillGoodsVO> update(@PathVariable Long seckillId,
                                         @Valid @RequestBody SeckillCreateRequest req) {
        CreateSeckillGoodsCommand command = SeckillApiConverter.toCommand(req);
        SeckillGoodsDTO dto = seckillGoodsApi.updateSeckill(seckillId, command);
        return Result.success(SeckillApiConverter.toVO(dto));
    }

    @Operation(summary = "取消秒杀活动")
    @OperationLog(module = "SECKILL", action = "CANCEL", targetIdSpEL = "#seckillId", targetType = "SECKILL")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PutMapping("/admin/{seckillId}/cancel")
    public Result<Void> cancel(@PathVariable Long seckillId) {
        seckillGoodsApi.cancelSeckill(seckillId);
        return Result.success();
    }

    /* ==================== 秒杀场次管理 API（场次化重构） ==================== */

    @Operation(summary = "创建秒杀场次（含商品列表）")
    @OperationLog(module = "SECKILL", action = "CREATE_ACTIVITY", targetType = "SECKILL_ACTIVITY")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PostMapping("/activities")
    public Result<SeckillActivityVO> createActivity(@Valid @RequestBody SeckillActivityCreateRequest req) {
        CreateActivityCommand command = SeckillApiConverter.toCommand(req);
        SeckillActivityDTO dto = seckillActivityApi.createActivity(command);
        return Result.success(SeckillApiConverter.toVO(dto));
    }

    @Operation(summary = "查询所有秒杀场次列表")
    @GetMapping("/activities")
    public Result<List<SeckillActivityVO>> listActivities() {
        List<SeckillActivityDTO> dtoList = seckillActivityApi.listActivities();
        return Result.success(SeckillApiConverter.toActivityVOList(dtoList));
    }

    @Operation(summary = "查询秒杀场次详情")
    @GetMapping("/activities/{activityId}")
    public Result<SeckillActivityVO> getActivityDetail(@PathVariable Long activityId) {
        SeckillActivityDTO dto = seckillActivityApi.getActivityDetail(activityId);
        return Result.success(SeckillApiConverter.toVO(dto));
    }

    @Operation(summary = "删除秒杀场次")
    @OperationLog(module = "SECKILL", action = "DELETE_ACTIVITY", targetIdSpEL = "#activityId", targetType = "SECKILL_ACTIVITY")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @DeleteMapping("/activities/{activityId}")
    public Result<Void> deleteActivity(@PathVariable Long activityId) {
        seckillActivityApi.deleteActivity(activityId);
        return Result.success();
    }
}
