package com.seckill.mall.upload.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传结果 DTO。
 *
 * <p>用于 Upload 模块 API 层对外暴露上传结果，替代直接暴露 VO。
 *
 * <p>来源映射：{@code UploadResultVO} → {@code UploadResultDTO}
 * （由 {@code UploadApiConverter.toDTO} 转换）。
 *
 * <p>参见 UPLOAD-API-CONTRACT.md。
 *
 * @author wnj
 * @since Phase U.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResultDTO {

    /**
     * 完整可访问 URL
     */
    private String url;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 图片宽度（像素），无法解析时为 null
     */
    private Integer width;

    /**
     * 图片高度（像素），无法解析时为 null
     */
    private Integer height;
}