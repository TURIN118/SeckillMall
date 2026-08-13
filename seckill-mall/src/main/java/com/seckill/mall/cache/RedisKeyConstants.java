package com.seckill.mall.cache;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RedisKeyConstants.java
 * 邮箱：nj651217@163.com
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    // L21 风格说明：所有 Key 常量统一使用 "业务域:子域:" 形式作为前缀，
    // 拼接具体 ID 后形成完整 Key。布隆过滤器为单一全局对象，不拼接 ID，
    // 故不以冒号结尾；其余常量均以冒号结尾以便拼接。
    public static final String SECKILL_GOODS = "seckill:goods:";
    public static final String SECKILL_STOCK = "seckill:stock:";
    public static final String SECKILL_BOUGHT = "seckill:bought:";
    public static final String SECKILL_MARK = "seckill:mark:";
    public static final String SECKILL_RESULT = "seckill:result:";
    public static final String SECKILL_INFO = "seckill:info:";
    public static final String SECKILL_TOKEN = "seckill:token:";
    /** 布隆过滤器为单一全局对象(无 ID 拼接)，故不以冒号结尾 */
    public static final String SECKILL_BLOOM_GOODS = "seckill:bloom:goods";
    public static final String RATE_SECKILL = "rate:seckill:";
    public static final String RATE_IP = "rate:ip:";
    /** AI 网关限流前缀：rate:ai:{caller}:{userId或ip} */
    public static final String RATE_AI = "rate:ai:";
    public static final String LOGIN_FAIL = "login:fail:";
    public static final String TOKEN_BLACKLIST = "token:blacklist:";
    public static final String CAPTCHA = "captcha:";
    public static final String MQ_CONSUMED = "mq:consumed:";

    public static String seckillGoods(Long goodsId) {
        return SECKILL_GOODS + goodsId;
    }

    public static String seckillStock(Long seckillId) {
        return SECKILL_STOCK + seckillId;
    }

    public static String seckillBought(Long seckillId) {
        return SECKILL_BOUGHT + seckillId;
    }

    public static String seckillResult(Long seckillId, Long userId) {
        return SECKILL_RESULT + seckillId + ":" + userId;
    }

    public static String seckillInfo(Long seckillId) {
        return SECKILL_INFO + seckillId;
    }

    public static String seckillToken(Long seckillId, Long userId) {
        return SECKILL_TOKEN + seckillId + ":" + userId;
    }

    public static String tokenBlacklist(String tokenId) {
        return TOKEN_BLACKLIST + tokenId;
    }

    public static String captcha(String captchaId) {
        return CAPTCHA + captchaId;
    }

    public static String mqConsumed(String messageId) {
        return MQ_CONSUMED + messageId;
    }
}
