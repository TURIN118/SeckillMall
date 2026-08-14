package com.seckill.mall.utils;

/**
 * 数据脱敏工具类。
 * 统一手机号/邮箱脱敏逻辑，供 UserConverter、UserServiceImpl 等复用。
 *
 * 创建人： @author WNJ
 * 项目名称： seckill-mall
 * 文件名称: DataMaskUtil.java
 * 邮箱: nj651217@163.com
 */
public final class DataMaskUtil {

    private DataMaskUtil() {}

    /**
     * 邮箱脱敏：保留首字符和 @ 后面域名，用户名其余部分用 *** 代替。
     * 示例：wang@example.com => w***@example.com
     * 空值或格式异常（@ 在开头或不存在）时返回原值。
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String prefix = email.substring(0, 1);
        String domain = email.substring(atIndex);
        return prefix + "***" + domain;
    }

    /**
     * 手机号脱敏：保留前3后4，中间用 **** 代替。
     * 示例：13812345678 => 138****5678
     * 空值或长度不足7时返回原值。
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 手机号脱敏（maskPhone 别名，向后兼容原 maskMobile 调用方）。
     *
     * @param mobile 原始手机号
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile) {
        return maskPhone(mobile);
    }
}
