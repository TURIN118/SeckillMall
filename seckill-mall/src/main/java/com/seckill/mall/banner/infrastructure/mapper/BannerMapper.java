package com.seckill.mall.banner.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.banner.infrastructure.entity.Banner;
import org.apache.ibatis.annotations.Mapper;

/**
 * 轮播图 Mapper
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BannerMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface BannerMapper extends BaseMapper<Banner> {
}