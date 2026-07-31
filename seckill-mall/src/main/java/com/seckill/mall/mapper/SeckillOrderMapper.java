package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.OrderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    SeckillOrder findByUserAndSeckill(@Param("userId") Long userId,
                                      @Param("seckillId") Long seckillId);

    IPage<SeckillOrder> selectOrderPage(IPage<SeckillOrder> page,
                                        @Param("userId") Long userId,
                                        @Param("seckillId") Long seckillId,
                                        @Param("status") OrderStatus status);
}
