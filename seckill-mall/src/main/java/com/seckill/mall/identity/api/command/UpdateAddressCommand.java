package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编辑收货地址命令。
 *
 * <p>业务语义：编辑收货地址（校验归属当前用户；若 isDefault 由 0 改 1 则先取消其他默认）。
 *
 * <p>原方法：{@code UserAddressService.update(Long, Long, UserAddressVO)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 地址 ID（必填） */
    private Long addressId;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人手机号 */
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String detailAddress;

    /** 是否默认 */
    private Integer isDefault;
}