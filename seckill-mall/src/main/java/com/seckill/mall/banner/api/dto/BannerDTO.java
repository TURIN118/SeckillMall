package com.seckill.mall.banner.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 轮播图 DTO。
 *
 * <p>用于 Banner 模块 API 层对外暴露轮播图数据，替代直接暴露 VO。
 *
 * <p>来源映射：{@code BannerVO} → {@code BannerDTO}
 * （由 {@code BannerApiConverter.toDTO} 转换）。
 *
 * <p>参见 BANNER-API-CONTRACT.md。
 *
 * @author wnj
 * @since Phase B.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerDTO {

    /** 主键ID */
    private Long id;

    /** 轮播图标题 */
    private String title;

    /** 图片URL */
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 排序权重（值越小越靠前） */
    private Integer sortOrder;

    /** 状态：1-启用 / 0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}