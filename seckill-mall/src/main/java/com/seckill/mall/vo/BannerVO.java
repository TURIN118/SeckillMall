package com.seckill.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图视图对象
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BannerVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class BannerVO {

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