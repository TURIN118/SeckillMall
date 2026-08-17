package com.seckill.mall.identity.domain;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户角色枚举。
 *
 * <p>从 {@code com.seckill.mall.entity.enums.UserRole} 迁移至 {@code identity.domain}。
 *
 * @author WNJ
 * @since Phase I.3
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