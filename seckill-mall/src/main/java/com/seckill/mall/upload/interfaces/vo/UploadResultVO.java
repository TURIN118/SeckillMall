package com.seckill.mall.upload.interfaces.vo;

import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UploadResultVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class UploadResultVO {

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