package com.seckill.mall.category.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.category.infrastructure.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    List<Category> selectByParentId(@Param("parentId") Long parentId);
}
