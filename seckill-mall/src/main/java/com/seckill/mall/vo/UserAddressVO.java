package com.seckill.mall.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址视图对象
 * <p>
 * 字段对应 {@code com.seckill.mall.entity.UserAddress}，使用驼峰命名，
 * 不暴露 isDeleted 等内部字段，仅返回前端展示所需的属性。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserAddressVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class UserAddressVO {

    /** 主键 ID */
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 收货人姓名 */
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    /** 收货人手机号 */
    @NotBlank(message = "收货人手机号不能为空")
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址（街道、门牌号等） */
    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    /** 是否默认地址：0-否 / 1-是 */
    private Integer isDefault;

    /** 创建时间 */
    private LocalDateTime createTime;
}