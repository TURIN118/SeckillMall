package com.seckill.mall.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 从购物车结算创建订单请求（需求13）
 * <p>
 * 用户在独立结算页提交，包含选中的购物车项ID列表与收货地址ID。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CartCheckoutRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CartCheckoutRequest {

    /** 收货地址ID */
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    /** 待结算的购物车项ID列表 */
    @NotEmpty(message = "待结算购物车项不能为空")
    private List<Long> cartIds;

    /** 备注（可选） */
    private String remark;

    /** 使用的用户优惠券ID（可选，不传表示不使用） */
    private Long userCouponId;
}