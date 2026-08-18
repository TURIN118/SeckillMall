package com.seckill.mall.system.interfaces.vo;

import lombok.Data;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderStatusDistributionVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OrderStatusDistributionVO {

    /**
     * 状态分布项列表，按 OrderStatus 枚举自然顺序输出
     */
    private List<StatusItem> items;

    /**
     * 订单总数（各状态 count 之和）
     */
    private Long total;

    /**
     * 单个状态分布项
     */
    @Data
    public static class StatusItem {

        /**
         * 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED
         */
        private String status;

        /**
         * 该状态订单数
         */
        private Long count;

        /**
         * 占比（0-100，保留 2 位小数）
         */
        private Double percentage;
    }
}