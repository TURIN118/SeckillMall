package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单明细视图对象
 * <p>
 * 与 {@code com.seckill.mall.entity.NormalOrderItem} 字段一致，作为
 * {@link NormalOrderDetailVO} 的订单明细列表元素载体，避免 VO 直接持有
 * Entity 引用而违反 ArchUnit VO→Entity 分层规则。
 * <p>
 * 字段名与 Entity 保持一致，确保 JSON 响应结构不变。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrderItemVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class NormalOrderItemVO {

    /** 明细ID */
    private Long id;

    /** 所属普通订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** SKU ID（下单时快照，null 表示无规格） */
    private Long skuId;

    /** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
    private String skuAttributes;

    /** 商品名称（下单时快照） */
    private String productName;

    /** 商品主图URL（下单时快照） */
    private String productImage;

    /** 商品单价（下单时快照，原价） */
    private BigDecimal unitPrice;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 = unitPrice × quantity */
    private BigDecimal subtotal;

    /** 逻辑删除标志 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}