package com.seckill.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductQueryRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductQueryRequest {

    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小不能小于 1")
    @Max(value = 100, message = "每页大小不能超过 100")
    private Integer pageSize = 10;

    private Long categoryId;

    /**
     * 价格区间筛选(可选)：原价下限，单位元。
     * null 表示不限制下限。
     */
    @Min(value = 0, message = "最低价格不能小于 0")
    private BigDecimal minPrice;

    /**
     * 价格区间筛选(可选)：原价上限，单位元。
     * null 表示不限制上限。
     */
    @Min(value = 0, message = "最高价格不能小于 0")
    private BigDecimal maxPrice;

    @Size(max = 100, message = "关键词最大 100 字符")
    private String keyword;

    /**
     * 排序字段(可选)。此处不做 @Pattern 校验，改在 Service 层做白名单过滤 + 别名归一化，
     * 既灵活(允许 salesCount/originalPrice/id 等前端常用别名)又安全(防 SQL 注入)。
     * Mapper XML 仅支持 price/sales/createTime 三个标准字段，其余值回退为 createTime。
     */
    private String sortBy;

    @Pattern(regexp = "^(asc|desc|ASC|DESC)?$",
            message = "排序方向仅支持 asc/desc")
    private String sortOrder;

    /**
     * 商品状态筛选(可选): ON_SALE / OFF_SHELF
     * null 或空字符串表示不筛选，返回所有状态商品(含下架)
     * 前台商品列表应传 "ON_SALE" 只展示上架商品
     * 后台商品管理默认不传以展示全部商品
     */
    @Pattern(regexp = "^(ON_SALE|OFF_SHELF)?$",
            message = "状态仅支持 ON_SALE/OFF_SHELF")
    private String status;
}
