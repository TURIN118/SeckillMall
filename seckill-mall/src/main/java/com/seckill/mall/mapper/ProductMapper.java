package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.enums.ProductStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    IPage<Product> selectProductPage(IPage<Product> page,
                                     @Param("categoryId") Long categoryId,
                                     @Param("keyword") String keyword,
                                     @Param("status") ProductStatus status,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);
}
