package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.service.BannerService;
import com.seckill.mall.vo.BannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 轮播图前台公开 Controller
 * 前缀：/api/v1/banners，无需登录
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BannerPublicController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "轮播图前台", description = "前台启用轮播图查询")
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerPublicController {

    private final BannerService bannerService;

    @Operation(summary = "查启用轮播图（前台首页）")
    @GetMapping("/active")
    public Result<List<BannerVO>> active() {
        return Result.success(bannerService.listActive());
    }
}