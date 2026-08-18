package com.seckill.mall.banner.interfaces.web;

import com.seckill.mall.banner.api.BannerApi;
import com.seckill.mall.banner.application.facade.BannerApiConverter;
import com.seckill.mall.banner.interfaces.vo.BannerVO;
import com.seckill.mall.common.Result;
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
 * Phase B.4 迁移：注入 BannerApi 替代 BannerService，通过 BannerApiConverter 适配 VO 契约。
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

    private final BannerApi bannerApi;

    @Operation(summary = "查启用轮播图（前台首页）")
    @GetMapping("/active")
    public Result<List<BannerVO>> active() {
        return Result.success(BannerApiConverter.toVOList(bannerApi.listActive()));
    }
}
