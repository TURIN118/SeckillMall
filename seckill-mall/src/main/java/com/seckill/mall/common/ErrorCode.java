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
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后再试"),

    // 用户相关
    USERNAME_OR_PASSWORD_ERROR(1003, "用户名或密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    CAPTCHA_ERROR(1005, "验证码错误或已过期"),
    USERNAME_EXISTS(1006, "用户名已存在"),
    PHONE_EXISTS(1007, "手机号已存在"),
    USER_NOT_FOUND(1008, "用户不存在"),
    PASSWORD_NOT_MATCH(1009, "两次密码不一致"),
    LOGIN_LOCKED(1010, "登录失败次数过多，账户已锁定30分钟"),
    REPLAY_DETECTED(1011, "请求签名校验失败"),

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
    CATEGORY_HAS_CHILDREN(4004, "分类下存在子分类，无法删除"),
    CATEGORY_HAS_PRODUCT(4005, "分类下存在商品，无法删除"),
    CATEGORY_CYCLE(4006, "不允许将分类移动到自身子分类下"),

    // 轮播图相关
    BANNER_NOT_FOUND(4101, "轮播图不存在"),

    // 文件上传相关
    UNSUPPORTED_MEDIA_TYPE(41501, "文件类型不支持"),
    FILE_TOO_LARGE(41502, "文件大小超限"),

    // 订单相关
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_ALREADY_PAID(3002, "订单已支付，请勿重复操作"),
    ORDER_STATUS_ERROR(3003, "订单状态异常"),
    ORDER_TIMEOUT(3004, "订单支付超时，已自动取消"),
    ORDER_CANCEL_FAILED(3005, "订单无法取消"),

    // 收货地址相关
    ADDRESS_NOT_FOUND(5001, "收货地址不存在"),
    ADDRESS_FORBIDDEN(5002, "无权操作该收货地址"),

    // 购物车相关
    CART_ITEM_NOT_FOUND(6001, "购物车项不存在"),
    CART_ITEM_FORBIDDEN(6002, "无权操作该购物车项"),
    CART_QUANTITY_INVALID(6003, "加购数量必须大于0"),

    // 收藏夹相关
    FAVORITE_ALREADY_EXIST(7001, "该商品已收藏"),
    FAVORITE_NOT_FOUND(7002, "收藏记录不存在"),

    // 优惠券相关
    COUPON_NOT_FOUND(8001, "优惠券不存在"),
    COUPON_DISABLED(8002, "优惠券已停用"),
    COUPON_EXPIRED(8003, "优惠券已过期"),
    COUPON_OUT_OF_STOCK(8004, "优惠券已抢完"),
    COUPON_ALREADY_RECEIVED(8005, "您已领取过该优惠券"),
    COUPON_NOT_STARTED(8006, "优惠券活动尚未开始"),

    // 充值卡相关
    RECHARGE_CARD_NOT_FOUND(9001, "充值卡不存在"),
    RECHARGE_CARD_USED(9002, "充值卡已被使用"),
    RECHARGE_CARD_DISABLED(9003, "充值卡已被禁用"),
    RECHARGE_CARD_PASSWORD_ERROR(9004, "卡密错误"),

    // 验证码相关
    VERIFICATION_CODE_INVALID(10001, "验证码错误或已过期"),
    VERIFICATION_CODE_SEND_FAILED(10002, "验证码发送失败"),
    VERIFICATION_CODE_RATE_LIMIT(10003, "发送过于频繁，请稍后再试"),

    // 钱包相关
    WALLET_BALANCE_NOT_ENOUGH(11001, "钱包余额不足"),
    WALLET_RECHARGE_FAILED(11002, "充值失败");

    private final int code;
    private final String message;
}
