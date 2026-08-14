package com.seckill.mall.ai.aigc.controller;

import com.seckill.mall.ai.aigc.dto.AigcGenerateRequest;
import com.seckill.mall.ai.aigc.service.AigcService;
import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AIGC 运营后台接口（T15）。
 * <p>管理员在商品编辑页一键 AI 生成文案（标题/卖点/详情/SEO），
 * 生成结果经内容安全过滤后返回前端预览，人工确认后才落库。
 *
 * <h3>接口说明</h3>
 * <ul>
 *   <li>路径：{@code POST /api/v1/admin/aigc/generate}</li>
 *   <li>鉴权：{@code @PreAuthorize("hasRole('ADMIN')")} 仅管理员可访问</li>
 *   <li>审计：{@code @OperationLog(module="PRODUCT", action="AIGC_GENERATE", targetType="PRODUCT")}
 *       记录管理员 AIGC 生成操作到操作日志</li>
 *   <li>限流：{@code @RateLimit(key="aigc", capacity=50, rate=20, seconds=3600)}
 *       令牌桶容量 50、补充速率 20/s、时间窗口 3600s，限制单 ADMIN 生成频次</li>
 * </ul>
 *
 * <h3>生成类型</h3>
 * <ul>
 *   <li>{@code TITLE} —— 商品标题（≤30 字）</li>
 *   <li>{@code DESCRIPTION} —— 商品简介/卖点短文（≤100 字）</li>
 *   <li>{@code DETAIL} —— 商品详情 HTML（{@code <ul>} 卖点 + {@code <table>} 参数）</li>
 *   <li>{@code SEO} —— SEO 关键词和描述 JSON</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AigcController.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/aigc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AigcController {

    private final AigcService aigcService;

    /**
     * 一键 AI 生成文案。
     * <p>请求体：{@code {"productId":1,"categoryId":2,"categoryName":"手机","skuAttributes":"{\"颜色\":\"黑\"}","price":1999,"generateType":"TITLE"}}
     * <p>响应：{@code Result<String>}，data 为生成并过滤后的文案。
     *
     * @param req 生成请求
     * @return 统一返回包装的文案
     */
    @PostMapping("/generate")
    @OperationLog(module = "PRODUCT", action = "AIGC_GENERATE", targetType = "PRODUCT")
    @RateLimit(key = "aigc", capacity = 50, rate = 20, seconds = 3600)
    public Result<String> generate(@Valid @RequestBody AigcGenerateRequest req) {
        log.info("AIGC 生成请求 generateType={} productId={}", req.getGenerateType(), req.getProductId());
        String result = aigcService.generate(req);
        return Result.success(result);
    }
}