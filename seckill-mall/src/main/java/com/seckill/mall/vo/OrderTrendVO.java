package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderTrendVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OrderTrendVO {

    /**
     * 日期列表，格式 yyyy-MM-dd
     */
    private List<String> dates;

    /**
     * 每日订单数列表，与 dates 一一对应
     */
    private List<Long> orderCounts;

    /**
     * 每日销售额列表（仅统计 PAID/COMPLETED），与 dates 一一对应
     */
    private List<BigDecimal> salesAmounts;
}