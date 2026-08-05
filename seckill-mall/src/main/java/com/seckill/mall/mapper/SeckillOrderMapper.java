package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.dto.AdminOrderQueryRequest;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.vo.AdminOrderVO;
import com.seckill.mall.vo.SeckillRankingVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    BigDecimal sumSalesAmount(@Param("statuses") List<OrderStatus> statuses);

    /**
     * 近 N 天订单趋势：按日期分组统计订单数与销售额（仅统计指定状态）
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @param statuses  需统计的状态列表
     * @return 每行包含 dt(日期)、cnt(订单数)、amt(销售额)
     */
    List<Map<String, Object>> selectOrderTrend(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("statuses") List<OrderStatus> statuses);

    /**
     * 订单状态分布：按 status 分组统计数量
     *
     * @param startTime 起始时间（含）
     * @param endTime   结束时间（含）
     * @return 每行包含 status(状态码)、cnt(订单数)
     */
    List<Map<String, Object>> selectStatusDistribution(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 后台订单高级筛选分页查询：关联 t_user/t_product/t_seckill_goods
     *
     * @param page 分页参数
     * @param req  查询条件（sortBy/sortOrder 已白名单归一化）
     * @return 分页结果
     */
    IPage<AdminOrderVO> selectAdminOrderPage(IPage<AdminOrderVO> page,
                                             @Param("req") AdminOrderQueryRequest req);

    /**
     * 今日订单数（create_time 落在当天）
     *
     * @param startDate 当天起始日期
     * @return 订单数
     */
    Long countTodayOrders(@Param("startDate") LocalDate startDate);

    /**
     * 秒杀排行榜 Top N：按销售额降序（销售额相同按销量降序）
     * 仅统计指定状态的订单，关联 t_seckill_goods 与 t_product 取商品名与秒杀价
     *
     * @param statuses 需统计的订单状态列表
     * @param limit    Top N
     * @return 排行榜列表
     */
    List<SeckillRankingVO> selectSeckillRanking(@Param("statuses") List<OrderStatus> statuses,
                                                @Param("limit") int limit);

    /**
     * 物理删除指定用户和秒杀活动的终态订单（TIMEOUT/CANCELLED）
     * 用于允许用户超时/取消后重新秒杀同一商品
     * 注意：使用物理删除而非逻辑删除，因为唯一索引 uk_user_seckill 不包含 is_deleted 字段
     */
    @Delete("DELETE FROM t_seckill_order WHERE user_id = #{userId} AND seckill_id = #{seckillId} " +
            "AND status IN ('TIMEOUT', 'CANCELLED')")
    int deleteTerminalOrders(@Param("userId") Long userId, @Param("seckillId") Long seckillId);
}
