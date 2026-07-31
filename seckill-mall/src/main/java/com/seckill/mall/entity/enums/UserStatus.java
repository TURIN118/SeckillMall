package com.seckill.mall.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserStatus.java
 * 邮箱：nj651217@163.com
 */
public enum UserStatus implements IEnum<String> {

    ACTIVE("ACTIVE", "启用"),
    DISABLED("DISABLED", "禁用");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
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

    public static UserStatus fromCode(String code) {
        for (UserStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }
}
