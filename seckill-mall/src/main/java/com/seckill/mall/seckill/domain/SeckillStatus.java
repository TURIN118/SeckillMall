package com.seckill.mall.seckill.domain;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillStatus.java
 * 邮箱：nj651217@163.com
 */
public enum SeckillStatus implements IEnum<String> {

    PENDING("PENDING", "未开始"),
    ACTIVE("ACTIVE", "进行中"),
    ENDED("ENDED", "已结束"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    SeckillStatus(String code, String description) {
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
    public static SeckillStatus fromCode(String code) {
        for (SeckillStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown SeckillStatus code: " + code);
    }
}
