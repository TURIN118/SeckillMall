package com.seckill.mall.vo;

import lombok.Data;

import java.util.List;

/**
 * 普通订单详情视图对象
 * <p>
 * 在普通订单视图基础上携带订单明细列表，用于订单详情展示。
 * 订单与明细分别使用 {@link NormalOrderVO} / {@link NormalOrderItemVO}
 * 独立 VO 承载，避免直接暴露 Entity 而违反 ArchUnit VO→Entity 分层规则。
 * 字段名与原 Entity 一致，确保 JSON 响应结构不变。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrderDetailVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class NormalOrderDetailVO {

    /** 订单基础信息 */
    private NormalOrderVO order;

    /** 订单明细列表 */
    private List<NormalOrderItemVO> items;

    /** 收货地址-收件人 */
    private String receiverName;
    /** 收货地址-手机号 */
    private String receiverPhone;
    /** 收货地址-省 */
    private String province;
    /** 收货地址-市 */
    private String city;
    /** 收货地址-区 */
    private String district;
    /** 收货地址-详细地址 */
    private String detailAddress;
}
