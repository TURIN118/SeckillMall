package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU 实体（属性值组合）
 * <p>
 * 对应表 {@code t_product_sku}，每个 SKU 拥有独立价格 / 库存 / 主图 / 编码 / 状态。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSku.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_product_sku")
public class ProductSku {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    /** SKU 编码，同商品内唯一，可空 */
    private String skuCode;

    /** SKU 价格 */
    private BigDecimal price;

    /** SKU 库存 */
    private Integer stock;

    /** SKU 主图 URL，null 表示沿用商品主图 */
    private String mainImage;

    /**
     * SKU 属性键值对 JSON 字符串
     * 如 {"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}
     * MySQL JSON 类型在 MyBatis-Plus 中按 String 读写
     */
    private String attributes;

    /** 状态：1-启用 / 0-禁用 */
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}