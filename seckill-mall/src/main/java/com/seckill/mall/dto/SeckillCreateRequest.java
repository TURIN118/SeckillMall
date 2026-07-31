package com.seckill.mall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillCreateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillCreateRequest {

    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    @NotBlank(message = "秒杀活动名称不能为空")
    @Size(max = 100, message = "秒杀活动名称最大 100 字符")
    private String seckillName;

    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价格必须大于 0")
    private BigDecimal seckillPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "秒杀库存至少为 1")
    private Integer stockCount;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private Integer perLimit = 1;

    private List<String> images;

    @Size(max = 500, message = "秒杀描述最大 500 字符")
    private String description;
}
