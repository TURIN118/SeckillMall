package com.seckill.mall.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券创建/更新请求
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponCreateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CouponCreateRequest {

    /** 优惠券名称 */
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "优惠券名称最长100字符")
    private String name;

    /** 类型：AMOUNT-满减 / DISCOUNT-折扣 */
    @NotBlank(message = "优惠券类型不能为空")
    @Pattern(regexp = "^(AMOUNT|DISCOUNT)$", message = "优惠券类型必须为 AMOUNT 或 DISCOUNT")
    private String type;

    /** 满减金额或折扣值（如 0.85 表示85折） */
    @NotNull(message = "金额/折扣值不能为空")
    @DecimalMin(value = "0", message = "金额/折扣值不能为负")
    private BigDecimal amount;

    /** 最低消费金额 */
    @NotNull(message = "最低消费金额不能为空")
    @DecimalMin(value = "0", message = "最低消费金额不能为负")
    private BigDecimal minAmount;

    /** 发放总数 */
    @NotNull(message = "发放总数不能为空")
    @Positive(message = "发放总数必须大于0")
    private Integer totalCount;

    /** 有效期开始 */
    @NotNull(message = "有效期开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 有效期结束 */
    @NotNull(message = "有效期结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 状态：1-启用 / 0-停用（可选，默认1） */
    private Integer status;

    /** 适用范围：ALL-全站 / CATEGORY-分类 / PRODUCT-商品（可选，默认 ALL） */
    private String scopeType;

    /** 适用分类ID（scopeType=CATEGORY时有效） */
    private Long categoryId;

    /** 适用商品ID（scopeType=PRODUCT时有效） */
    private Long productId;
}