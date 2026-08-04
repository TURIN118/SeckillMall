package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.enums.ProductStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 商品分页查询。
     *
     * @param categoryId   精确匹配的二级分类 ID（一级分类展开场景应传 null 并改用 categoryIds）
     * @param categoryIds  分类 ID 集合（一级分类展开后其所有二级分类 ID），非空时按 IN 查询
     * @param minPrice     原价下限(可选)
     * @param maxPrice     原价上限(可选)
     */
    IPage<Product> selectProductPage(IPage<Product> page,
                                     @Param("categoryId") Long categoryId,
                                     @Param("categoryIds") List<Long> categoryIds,
                                     @Param("keyword") String keyword,
                                     @Param("status") ProductStatus status,
                                     @Param("minPrice") BigDecimal minPrice,
                                     @Param("maxPrice") BigDecimal maxPrice,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);
}
