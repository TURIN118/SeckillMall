package com.seckill.mall.payment.domain;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 充值卡状态枚举
 * <ul>
 *   <li>{@link #UNUSED}    未使用</li>
 *   <li>{@link #USED}      已使用</li>
 *   <li>{@link #DISABLED}  已禁用</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardStatus.java
 * 邮箱：nj651217@163.com
 */
public enum RechargeCardStatus implements IEnum<String> {

    UNUSED("UNUSED", "未使用"),
    USED("USED", "已使用"),
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String description;

    RechargeCardStatus(String code, String description) {
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

    public static RechargeCardStatus fromCode(String code) {
        for (RechargeCardStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown RechargeCardStatus code: " + code);
    }
}