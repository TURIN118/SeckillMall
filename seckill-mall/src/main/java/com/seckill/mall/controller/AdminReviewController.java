package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.service.ProductReviewService;
import com.seckill.mall.vo.ProductReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminReviewController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "后台评论管理", description = "评论列表/回复/隐藏显示")
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ProductReviewService productReviewService;

    @Operation(summary = "查所有评论（可按 status 筛选）")
    @GetMapping("/list")
    public Result<PageResult<ProductReviewVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(productReviewService.listAll(status, pageNum, pageSize));
    }

    @Operation(summary = "回复评论")
    @OperationLog(module = "REVIEW", action = "REPLY", targetIdSpEL = "#id", targetType = "REVIEW")
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id,
                              @Validated @RequestBody ReplyRequest req) {
        productReviewService.reply(id, req.getReplyContent());
        return Result.success();
    }

    @Operation(summary = "隐藏/显示评论")
    @OperationLog(module = "REVIEW", action = "UPDATE_STATUS", targetIdSpEL = "#id", targetType = "REVIEW")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Validated @RequestBody StatusRequest req) {
        productReviewService.updateStatus(id, req.getStatus());
        return Result.success();
    }

    /**
     * 回复评论请求体
     */
    @Data
    public static class ReplyRequest {
        @NotBlank(message = "回复内容不能为空")
        private String replyContent;
    }

    /**
     * 状态更新请求体
     */
    @Data
    public static class StatusRequest {
        @NotNull(message = "状态不能为空")
        private Integer status;
    }
}