package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillResultVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillResultVO {

    /**
     * 0=排队中 / 1=成功 / -1=失败
     */
    private Integer status;

    private String requestId;

    private Long orderId;

    private String orderNo;

    private BigDecimal totalAmount;

    private LocalDateTime payExpireTime;
}
