package com.seckill.mall.mq.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderDelayMessage.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OrderDelayMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private String orderNo;

    private Long expireTime;

    /** 订单类型：SECKILL=秒杀订单，NORMAL=普通订单 */
    private String orderType;
}
