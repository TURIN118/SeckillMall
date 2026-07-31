package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：DashboardVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class DashboardVO {

    private Long userCount;

    private Long orderCount;

    private BigDecimal totalSales;

    private Long seckillCount;
}
