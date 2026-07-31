package com.seckill.mall.controller;

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
@Tag(name = "秒杀活动")
@RestController
@RequestMapping("/api/v1/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillGoodsService seckillGoodsService;
    private final SeckillService seckillService;
    private final SeckillTokenService seckillTokenService;

    @Operation(summary = "秒杀活动列表")
    @GetMapping("/list")
    public Result<PageResult<SeckillGoodsVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(seckillGoodsService.listSeckill(status, pageNum, pageSize));
    }

    @Operation(summary = "秒杀活动详情")
    @GetMapping("/{seckillId}")
    public Result<SeckillGoodsVO> detail(@PathVariable Long seckillId) {
        return Result.success(seckillGoodsService.getSeckillDetail(seckillId));
    }

    @Operation(summary = "获取秒杀令牌")
    @GetMapping("/{seckillId}/token")
    public Result<String> token(@PathVariable Long seckillId) {
        return Result.success(seckillTokenService.getSeckillToken(seckillId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "查询实时库存")
    @GetMapping("/{seckillId}/stock")
    public Result<Integer> stock(@PathVariable Long seckillId) {
        return Result.success(seckillGoodsService.getStock(seckillId));
    }

    @Operation(summary = "执行秒杀")
    @PostMapping("/{seckillId}")
    public Result<SeckillResultVO> doSeckill(
            @PathVariable Long seckillId,
            @RequestHeader(value = "X-Seckill-Token", required = false) String headerToken,
            @RequestParam(value = "seckillToken", required = false) String paramToken) {
        String token = headerToken != null ? headerToken : paramToken;
        return Result.success(seckillService.doSeckill(seckillId, token));
    }

    @Operation(summary = "查询秒杀结果")
    @GetMapping("/{seckillId}/result")
    public Result<SeckillResultVO> result(
            @PathVariable Long seckillId,
            @RequestParam String requestId) {
        return Result.success(seckillService.getSeckillResult(seckillId, requestId));
    }

    @Operation(summary = "创建秒杀活动")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PostMapping("/admin")
    public Result<SeckillGoodsVO> create(@Valid @RequestBody SeckillCreateRequest req) {
        return Result.success(seckillGoodsService.createSeckill(req));
    }

    @Operation(summary = "编辑秒杀活动")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PutMapping("/admin/{seckillId}")
    public Result<SeckillGoodsVO> update(@PathVariable Long seckillId,
                                         @Valid @RequestBody SeckillCreateRequest req) {
        return Result.success(seckillGoodsService.updateSeckill(seckillId, req));
    }

    @Operation(summary = "取消秒杀活动")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PutMapping("/admin/{seckillId}/cancel")
    public Result<Void> cancel(@PathVariable Long seckillId) {
        seckillGoodsService.cancelSeckill(seckillId);
        return Result.success();
    }
}
