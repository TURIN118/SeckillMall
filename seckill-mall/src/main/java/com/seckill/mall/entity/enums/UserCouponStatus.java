package com.seckill.mall.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户优惠券状态枚举
 * <ul>
 *   <li>{@link #UNUSED}  未使用</li>
 *   <li>{@link #USED}    已使用</li>
 *   <li>{@link #EXPIRED} 已过期</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserCouponStatus.java
 * 邮箱：nj651217@163.com
 */
public enum UserCouponStatus implements IEnum<String> {

    UNUSED("UNUSED", "未使用"),
    USED("USED", "已使用"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String description;

    UserCouponStatus(String code, String description) {
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

    public static UserCouponStatus fromCode(String code) {
        for (UserCouponStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown UserCouponStatus code: " + code);
    }
}