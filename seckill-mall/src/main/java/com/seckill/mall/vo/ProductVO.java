package com.seckill.mall.vo;

import com.seckill.mall.entity.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductVO {

    private Long id;

    private String productName;

    private Long categoryId;

    private String categoryName;

    private String description;

    private BigDecimal originalPrice;

    private List<String> images;

    private Integer stock;

    private Integer salesCount;

    private ProductStatus status;

    private LocalDateTime createTime;
}
