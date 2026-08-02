package com.seckill.mall.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势数据项 VO
 *
 * <p>用于用户注册趋势、订单趋势等按日期分组的统计接口，
 * 每一项表示某一天（或某一时间粒度）的数量。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TrendItemVO.java
 * 邮箱：nj651217@163.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendItemVO {

    /**
     * 日期字符串，格式 yyyy-MM-dd
     */
    private String date;

    /**
     * 当日数量（注册数 / 订单数等，由调用方语义决定）
     */
    private Long count;
}