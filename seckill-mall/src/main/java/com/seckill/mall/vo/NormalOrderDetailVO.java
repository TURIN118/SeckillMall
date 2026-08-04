package com.seckill.mall.vo;

import com.seckill.mall.entity.NormalOrder;
import com.seckill.mall.entity.NormalOrderItem;
import lombok.Data;

import java.util.List;

/**
 * 普通订单详情视图对象
 * <p>
 * 在普通订单实体基础上携带订单明细列表，用于订单详情展示。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrderDetailVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class NormalOrderDetailVO {

    /** 订单基础信息 */
    private NormalOrder order;

    /** 订单明细列表 */
    private List<NormalOrderItem> items;
}