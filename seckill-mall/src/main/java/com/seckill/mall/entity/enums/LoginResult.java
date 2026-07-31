package com.seckill.mall.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：LoginResult.java
 * 邮箱：nj651217@163.com
 */
public enum LoginResult implements IEnum<String> {

    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    LoginResult(String code, String description) {
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

    public static LoginResult fromCode(String code) {
        for (LoginResult e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown LoginResult code: " + code);
    }
}
