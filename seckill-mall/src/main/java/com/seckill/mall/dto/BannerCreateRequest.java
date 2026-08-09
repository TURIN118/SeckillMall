package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 轮播图新增请求 DTO。
 * <p>
 * M-D1 修复：从 BannerVO 拆出请求体，避免 VO 双向用作请求体与响应体。
 * M-S2 修复：补 jakarta.validation 约束，配合 Controller @Valid 触发校验。
 * H-S1 修复：URL 字段限制协议白名单（http/https）。
 * M-F10 修复：title 强制非空。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BannerCreateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class BannerCreateRequest {

    /** 轮播图标题（M-F10：强制非空，最长 100） */
    @NotBlank(message = "轮播图标题不能为空")
    @Size(max = 100, message = "轮播图标题最长 100 字符")
    private String title;

    /** 图片URL（H-S1：协议白名单 http/https，或站内相对路径以 / 开头） */
    @NotBlank(message = "图片URL不能为空")
    @Size(max = 500, message = "图片URL最长 500 字符")
    @Pattern(regexp = "^(https?://|/).*", message = "图片URL必须以 http://、https:// 或 / 开头")
    private String imageUrl;

    /** 点击跳转链接（可选，但若提供必须 http/https 或站内相对路径以 / 开头） */
    @Size(max = 500, message = "跳转链接最长 500 字符")
    @Pattern(regexp = "^$|^(https?://|/).*", message = "跳转链接必须以 http://、https:// 或 / 开头")
    private String linkUrl;

    /** 排序权重（值越小越靠前），可选，默认 0 */
    private Integer sortOrder;

    /** 状态：1-启用 / 0-禁用，可选，默认 1 */
    private Integer status;
}