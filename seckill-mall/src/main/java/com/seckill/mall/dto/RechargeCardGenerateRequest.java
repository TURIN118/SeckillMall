package com.seckill.mall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值卡批量生成请求
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardGenerateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class RechargeCardGenerateRequest {

    /** 面额 */
    @NotNull(message = "面额不能为空")
    @DecimalMin(value = "0.01", message = "面额必须大于0")
    private BigDecimal faceValue;

    /** 生成数量 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 1000, message = "单次生成数量不超过1000")
    private Integer count;
}