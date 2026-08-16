package com.seckill.mall.vo;

import com.seckill.mall.entity.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单视图对象
 * <p>
 * 与 {@code com.seckill.mall.entity.NormalOrder} 字段一致，作为
 * {@link NormalOrderDetailVO} 的订单基础信息载体，避免 VO 直接持有
 * Entity 引用而违反 ArchUnit VO→Entity 分层规则。
 * <p>
 * 字段名与 Entity 保持一致，确保 JSON 响应结构不变。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrderVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class NormalOrderVO {

    /** 订单ID */
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

    /** 逻辑删除标志 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}