package com.seckill.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductReviewVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductReviewVO {

    private Long id;

    private Long productId;

    /** SKU ID，null 表示无规格评论 */
    private Long skuId;

    /** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
    private String skuAttributes;

    private Long userId;

    /** 评论用户名（JOIN 用户表获取） */
    private String userName;

    private Long orderId;

    private String content;

    /** 评分：1-5 星 */
    private Integer rating;

    /** 评论图片 URL 数组 */
    private List<String> images;

    /** 状态：1-显示 / 0-隐藏 */
    private Integer status;

    /** 商家回复内容 */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}