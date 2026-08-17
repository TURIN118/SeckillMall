package com.seckill.mall.identity.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.identity.infrastructure.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper。
 *
 * <p>从 {@code com.seckill.mall.mapper.UserMapper} 迁移至 {@code identity.infrastructure.mapper}。
 * 仅在 identity 模块 infrastructure 层内部使用，不对外暴露。
 *
 * @author WNJ
 * @since Phase I.3
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(@Param("username") String username);

    User findByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱查询用户（未删除）
     *
     * @param email 邮箱
     * @return 用户实体，不存在返回 null
     */
    User findByEmail(@Param("email") String email);

    /**
     * Bug12修复：根据邮箱查询用户列表（未删除）。
     * <p>
     * 数据库中 email 可能存在重复记录，使用 selectOne 会抛 TooManyResultsException。
     * 改用 selectList 由调用方取第一条，避免异常。
     *
     * @param email 邮箱
     * @return 用户实体列表，不存在返回空列表
     */
    List<User> findListByEmail(@Param("email") String email);

    /**
     * Bug12修复：根据手机号查询用户列表（未删除）。
     * <p>
     * 与 findListByEmail 同理，避免手机号重复时 selectOne 抛 TooManyResultsException。
     *
     * @param phone 手机号
     * @return 用户实体列表，不存在返回空列表
     */
    List<User> findListByPhone(@Param("phone") String phone);

    /**
     * 用户注册趋势：按日期分组统计注册数
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每行包含 dt(日期)、cnt(注册数)
     */
    List<Map<String, Object>> selectUserTrend(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * 今日注册用户数（create_time 落在当天）
     *
     * @param startDate 当天起始日期
     * @return 注册数
     */
    Long countTodayRegistered(@Param("startDate") LocalDate startDate);

    /**
     * 钱包扣款：原子扣减用户余额，仅在余额充足时扣减成功。
     * <p>
     * 通过 {@code WHERE balance >= amount} 保证并发安全，避免余额负数。
     *
     * @param userId 用户 ID
     * @param amount 扣减金额（必须大于0）
     * @return 受影响行数：1=扣减成功，0=余额不足或用户不存在
     */
    @Update("UPDATE t_user SET balance = balance - #{amount}, update_time = NOW() " +
            "WHERE id = #{userId} AND is_deleted = 0 AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}