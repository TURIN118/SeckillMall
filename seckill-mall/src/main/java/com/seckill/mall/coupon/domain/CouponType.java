package com.seckill.mall.coupon.domain;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 优惠券类型枚举
 * <ul>
 *   <li>{@link #AMOUNT}  满减券：{@code amount} 为满减金额（如 10 表示满10元减10元）</li>
 *   <li>{@link #DISCOUNT} 折扣券：{@code amount} 为折扣值（如 0.85 表示85折）</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponType.java
 * 邮箱：nj651217@163.com
 */
public enum CouponType implements IEnum<String> {

    AMOUNT("AMOUNT", "满减"),
    DISCOUNT("DISCOUNT", "折扣");

    private final String code;
    private final String description;

    CouponType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getValue() {
        return code;
    }

    public static CouponType fromCode(String code) {
        for (CouponType e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown CouponType code: " + code);
    }
}