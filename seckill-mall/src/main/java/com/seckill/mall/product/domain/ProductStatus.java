package com.seckill.mall.product.domain;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductStatus.java
 * 邮箱：nj651217@163.com
 */
public enum ProductStatus implements IEnum<String> {

    ON_SALE("ON_SALE", "在售"),
    OFF_SHELF("OFF_SHELF", "下架");

    private final String code;
    private final String description;

    ProductStatus(String code, String description) {
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

    public static ProductStatus fromCode(String code) {
        for (ProductStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown ProductStatus code: " + code);
    }
}
