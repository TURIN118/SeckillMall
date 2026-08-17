package com.seckill.mall.product.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.product.infrastructure.entity.ProductAttribute;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品属性 Mapper
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductAttributeMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface ProductAttributeMapper extends BaseMapper<ProductAttribute> {
}