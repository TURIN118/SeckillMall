package com.seckill.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 立即购买创建订单请求（需求5）
 * <p>
 * 用户在商品详情页点击"立即购买"时提交，包含购买的商品ID与数量。
 * 收货地址ID由前端在确认弹窗中选择后随请求一起提交。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BuyNowRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class BuyNowRequest {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** SKU ID（可选，null 表示无规格商品立即购买） */
    private Long skuId;

    /** 购买数量 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    /** 收货地址ID */
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    /** 备注（可选） */
    private String remark;
}