package com.seckill.mall.controller;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.ProductReviewService;
import com.seckill.mall.vo.ProductReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductReviewController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "商品评论", description = "前台商品评论查询/发表")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @Operation(summary = "查商品评论分页（公开接口）")
    @GetMapping("/product/{productId}")
    public Result<PageResult<ProductReviewVO>> listByProduct(
            @PathVariable Long productId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(productReviewService.listByProductId(productId, pageNum, pageSize));
    }

    @Operation(summary = "发表评论（需登录）")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @PostMapping("/create")
    public Result<ProductReviewVO> create(@Validated @RequestBody ReviewCreateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        // 安全修复（M5）：对评论内容做 HTML 转义，防止存储型 XSS
        String safeContent = HtmlUtils.htmlEscape(req.getContent());
        return Result.success(productReviewService.create(
                userId, req.getProductId(), safeContent, req.getRating(), req.getImages()));
    }

    /**
     * 发表评论请求体
     */
    @Data
    public static class ReviewCreateRequest {
        @NotNull(message = "商品 ID 不能为空")
        private Long productId;

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容最大 1000 字符")
        private String content;

        @NotNull(message = "评分不能为空")
        private Integer rating;

        /** 评论图片 URL 数组（JSON 字符串），可选 */
        private String images;
    }
}