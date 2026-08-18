package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.seckill.infrastructure.entity.SeckillGoods;
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
                                          @Param("keyword") String keyword,
                                          @Param("categoryId") Long categoryId);

    List<SeckillGoods> selectActiveList();

    /**
     * 乐观锁扣减库存：以 available_count>0 作为乐观条件，原子减一。
     * 返回受影响行数（1=成功，0=库存不足或活动不存在）。
     */
    int deductStockOptimistic(@Param("id") Long id);

    /**
     * H-C1 修复：乐观锁回补库存：available_count + 1，条件 available_count < stock_count 防超溢。
     * 用于秒杀订单取消/超时后回补 DB available_count。
     *
     * @param id 秒杀商品 ID
     * @return 受影响行数（1=成功，0=活动不存在或库存已满）
     */
    int restoreStockOptimistic(@Param("id") Long id);

    /**
     * Bug6修复：将已到开始时间但仍为PENDING状态的秒杀活动更新为ACTIVE。
     * 条件：status=PENDING AND start_time <= NOW() AND end_time > NOW() AND is_deleted=0
     *
     * @return 受影响行数
     */
    int updatePendingToActive();

    /**
     * Bug6修复：将已过结束时间的ACTIVE秒杀活动更新为ENDED。
     * 条件：status=ACTIVE AND end_time <= NOW() AND is_deleted=0
     *
     * @return 受影响行数
     */
    int updateActiveToEnded();
}
