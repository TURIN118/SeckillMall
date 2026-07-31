package com.seckill.mall.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserRole.java
 * 邮箱：nj651217@163.com
 */
public enum UserRole implements IEnum<String> {

    BUYER("BUYER", "买家"),
    SELLER("SELLER", "卖家"),
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
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

    public static UserRole fromCode(String code) {
        for (UserRole e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown UserRole code: " + code);
    }
}
