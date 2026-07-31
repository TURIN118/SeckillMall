package com.seckill.mall.service;

import com.seckill.mall.vo.CategoryVO;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryService.java
 * 邮箱：nj651217@163.com
 */
public interface CategoryService {

    List<CategoryVO> getCategoryTree();
}
