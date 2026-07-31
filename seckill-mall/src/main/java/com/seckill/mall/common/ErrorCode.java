package com.seckill.mall.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ErrorCode.java
 * 邮箱：nj651217@163.com
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "success"),

    // 通用错误
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无权限访问"),
    SYSTEM_ERROR(500, "系统繁忙"),

    // 用户相关
    USERNAME_OR_PASSWORD_ERROR(1003, "用户名或密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    CAPTCHA_ERROR(1005, "验证码错误或已过期"),
    USERNAME_EXISTS(1006, "用户名已存在"),
    PHONE_EXISTS(1007, "手机号已存在"),
    USER_NOT_FOUND(1008, "用户不存在"),
    PASSWORD_NOT_MATCH(1009, "两次密码不一致"),
    LOGIN_LOCKED(1010, "登录失败次数过多，账户已锁定30分钟"),

    // 秒杀相关
    SECKILL_NOT_FOUND(2001, "秒杀活动不存在"),
    SECKILL_NOT_STARTED(2002, "秒杀活动尚未开始"),
    STOCK_EMPTY(2003, "库存不足，商品已售罄"),
    REPEAT_SECKILL(2004, "您已参与过此活动，每位用户限购一次"),
    SECKILL_ENDED(2005, "秒杀活动已结束"),
    SECKILL_TOKEN_INVALID(2006, "秒杀令牌无效或已过期"),
    SECKILL_TOO_MANY(2007, "秒杀活动已取消"),

    // 商品与分类相关
    PRODUCT_NOT_FOUND(4001, "商品不存在"),
    CATEGORY_NOT_FOUND(4002, "分类不存在"),
    CATEGORY_DISABLED(4003, "分类已禁用"),

    // 订单相关
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_ALREADY_PAID(3002, "订单已支付，请勿重复操作"),
    ORDER_STATUS_ERROR(3003, "订单状态异常"),
    ORDER_TIMEOUT(3004, "订单支付超时，已自动取消"),
    ORDER_CANCEL_FAILED(3005, "订单无法取消");

    private final int code;
    private final String message;
}
