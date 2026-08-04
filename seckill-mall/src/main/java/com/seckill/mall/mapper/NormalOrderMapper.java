package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.NormalOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 普通订单 Mapper
 * <p>
 * 基于 MyBatis-Plus {@link BaseMapper}，复杂查询使用 Lambda Wrapper。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrderMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface NormalOrderMapper extends BaseMapper<NormalOrder> {
}