package com.seckill.mall.product.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductReview.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_product_review")
public class ProductReview {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    /** SKU ID，null 表示无规格评论 */
    private Long skuId;

    /** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
    private String skuAttributes;

    private Long userId;

    private Long orderId;

    private String content;

    /** 评分：1-5 星 */
    private Integer rating;

    /** 评论图片 URL 数组（JSON 存为字符串） */
    private String images;

    /** 状态：1-显示 / 0-隐藏 */
    private Integer status;

    /** 商家回复内容 */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}