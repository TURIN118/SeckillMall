package com.seckill.mall.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderStatus.java
 * 邮箱：nj651217@163.com
 */
public enum OrderStatus implements IEnum<String> {

    UNPAID("UNPAID", "待支付"),
    PAID("PAID", "已支付"),
    SHIPPED("SHIPPED", "已发货"),
    CANCELLED("CANCELLED", "已取消"),
    TIMEOUT("TIMEOUT", "已超时"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
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

    /**
     * L22 修复：添加 @JsonCreator 使前端传入 code 字符串时能正确反序列化为枚举。
     */
    @JsonCreator
    public static OrderStatus fromCode(String code) {
        for (OrderStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown OrderStatus code: " + code);
    }
}
