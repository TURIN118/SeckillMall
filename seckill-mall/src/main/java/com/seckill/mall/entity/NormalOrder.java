package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单实体
 * <p>
 * 对应表 {@code t_normal_order}，由"立即购买（需求5）"与
 * "购物车结算（需求13）"两种入口创建。与秒杀订单 {@link SeckillOrder}
 * 是两套独立模型，互不干扰。
 * <p>
 * 一个普通订单可包含多个商品明细，参见 {@link NormalOrderItem}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrder.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_normal_order")
public class NormalOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单号（唯一，时间戳+随机串） */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** 收货地址ID */
    private Long addressId;

    /** 商品总金额（明细小计之和） */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 实付金额 = totalAmount + freightAmount */
    private BigDecimal payAmount;

    /** 订单状态：UNPAID/PAID/SHIPPED/CANCELLED/TIMEOUT/COMPLETED */
    @TableField("status")
    private OrderStatus status;

    /** 物流公司 */
    private String shippingCompany;

    /** 快递单号 */
    private String shippingNo;

    /** 支付方式：WALLET/ALIPAY/WECHAT 等 */
    private String payMethod;

    /** 第三方/钱包支付流水号 */
    private String transactionId;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 支付截止时间（超时自动取消） */
    private LocalDateTime payExpireTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 确认收货时间 */
    private LocalDateTime confirmTime;

    /** 备注（用户下单时填写，可选） */
    private String remark;

    /** 使用的用户优惠券ID */
    private Long userCouponId;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}