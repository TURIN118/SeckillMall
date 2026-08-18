package com.seckill.mall.seckill.infrastructure.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderMessage.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long seckillId;

    private Long userId;

    private String requestId;

    private Long timestamp;
}
