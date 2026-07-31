package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.enums.SeckillStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillGoodsMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    IPage<SeckillGoods> selectSeckillPage(IPage<SeckillGoods> page,
                                          @Param("status") SeckillStatus status,
                                          @Param("keyword") String keyword);

    List<SeckillGoods> selectActiveList();

    /**
     * 乐观锁扣减库存：以 available_count>0 作为乐观条件，原子减一。
     * 返回受影响行数（1=成功，0=库存不足或活动不存在）。
     */
    int deductStockOptimistic(@Param("id") Long id);
}
