package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增收货地址命令。
 *
 * <p>业务语义：新增收货地址（若用户此前无地址则自动设为默认；若 isDefault=1 则先取消其他默认）。
 *
 * <p>原方法：{@code UserAddressService.create(Long, UserAddressVO)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAddressCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 收件人姓名（必填） */
    private String receiverName;

    /** 收件人手机号（必填） */
    private String receiverPhone;

    /** 省份（必填） */
    private String province;

    /** 城市（必填） */
    private String city;

    /** 区/县（必填） */
    private String district;

    /** 详细地址（必填） */
    private String detailAddress;

    /** 是否默认（0/1，默认 0） */
    private Integer isDefault;
}