package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发货请求参数
 * <p>
 * 管理员发货时需填写物流公司与快递单号，
 * 后端校验必填及长度限制。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ShipRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ShipRequest {

    /** 物流公司（必填） */
    @NotBlank(message = "物流公司不能为空")
    @Size(max = 50, message = "物流公司名称不能超过50个字符")
    private String shippingCompany;

    /** 快递单号（必填） */
    @NotBlank(message = "快递单号不能为空")
    @Size(max = 64, message = "快递单号不能超过64个字符")
    private String shippingNo;
}