package com.seckill.mall.utils;

/**
 * 创建人： @author WNJ
 * 项目名称： seckill-mall
 * 文件名称: MaskUtil.java
 * 邮箱: nj651217@163.com
 */
public class DataMaskUtil {

    /**
     * 邮箱脱敏：保留首字符和@后面域名，用户名其余部分用 *** 代替
     * 示例：wang@example.com  =>  w***@example.com
     *       空值或格式异常时返回空串
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        // 不存在 @ 或 @ 在开头，无法脱敏，直接返回原值或空
        if (atIndex <= 0) {
            return email;   // 或根据安全要求返回空
        }
        String prefix = email.substring(0, 1);   // 第一个字符
        String domain = email.substring(atIndex); // @及后面部分
        return prefix + "***" + domain;
    }

    /**
     * 手机号脱敏：保留前3后4，中间用 **** 代替
     * 示例：13812345678  =>  138****5678
     *      异常情况（长度不足/空）返回原值或空
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return "";
        }
        // 仅对11位标准手机号处理，其他直接脱敏后三段
        if (mobile.length() == 11) {
            return mobile.substring(0, 3) + "****" + mobile.substring(7);
        }
        // 非标准长度简单脱敏：保留首字符和末四位
        if (mobile.length() > 4) {
            return mobile.charAt(0) + "****" + mobile.substring(mobile.length() - 4);
        }
        // 长度过短，无法脱敏，返回掩码
        return "****";
    }
}
