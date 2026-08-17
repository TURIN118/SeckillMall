package com.seckill.mall.identity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收货地址 DTO，替代 UserAddress Entity 跨模块传递。
 *
 * <p>跨模块只读传递时使用，避免暴露 {@code UserAddress} Entity。
 * 裁剪掉 {@code isDeleted}、{@code createTime}、{@code updateTime} 等基础设施字段。
 *
 * <p>来源映射：UserAddress Entity → AddressDTO
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    /** 地址 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

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

    /** 是否默认（0/1） */
    private Integer isDefault;
}