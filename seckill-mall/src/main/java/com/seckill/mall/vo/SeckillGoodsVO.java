package com.seckill.mall.vo;

import com.seckill.mall.seckill.domain.SeckillStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillGoodsVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillGoodsVO {

    private Long id;

    private Long productId;

    private String productName;

    private String seckillName;

    private BigDecimal seckillPrice;

    private Integer stockCount;

    private Integer availableCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private SeckillStatus status;

    private Integer perLimit;

    private List<String> images;

    private String description;

    private LocalDateTime createTime;
}
