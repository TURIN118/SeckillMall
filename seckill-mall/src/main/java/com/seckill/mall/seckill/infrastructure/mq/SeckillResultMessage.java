package com.seckill.mall.seckill.infrastructure.mq;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillResultMessage.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillResultMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long seckillId;

    /**
     * 0=排队中 / 1=成功 / -1=失败
     */
    private Integer status;

    private Long orderId;

    private String orderNo;

    private BigDecimal totalAmount;
}
