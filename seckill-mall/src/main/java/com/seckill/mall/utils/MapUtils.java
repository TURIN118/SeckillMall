package com.seckill.mall.utils;

import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Map 工具类。
 * 提取 AI Advisor 中冗余的 readString/readLong 方法，统一维护。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：MapUtils.java
 * 邮箱：nj651217@163.com
 */
public final class MapUtils {

    private MapUtils() {}

    /**
     * 从 Map 读取字符串值，缺失/空白时返回默认值。
     * @param ctx 源 Map（可为 null）
     * @param key 键名
     * @param defaultValue 默认值
     * @return 字符串值（trim 后），null/空白返回 defaultValue
     */
    public static String readString(Map<String, Object> ctx, String key, String defaultValue) {
        if (ctx == null) {
            return defaultValue;
        }
        Object value = ctx.get(key);
        if (value == null) {
            return defaultValue;
        }
        String s = String.valueOf(value).trim();
        return StringUtils.hasText(s) ? s : defaultValue;
    }

    /**
     * 从 Map 读取 Long 值，缺失/空白/解析失败时返回 null。
     * @param ctx 源 Map（可为 null）
     * @param key 键名
     * @return Long 值，解析失败返回 null
     */
    public static Long readLong(Map<String, Object> ctx, String key) {
        if (ctx == null) {
            return null;
        }
        Object value = ctx.get(key);
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}