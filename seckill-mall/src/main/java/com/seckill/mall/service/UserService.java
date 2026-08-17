package com.seckill.mall.service;

import com.seckill.mall.identity.infrastructure.entity.User;
import com.seckill.mall.vo.UserVO;

/**
 * 用户个人信息服务接口
 * <p>
 * M-D2 修复：将 {@code UserController} 中对 {@code UserMapper} 的直接调用下沉到 Service，
 * Controller 仅负责编排并返回 {@code Result<VO>}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserService.java
 * 邮箱：nj651217@163.com
 */
public interface UserService {

    /**
     * 修改手机号（调用方需先完成验证码校验）
     *
     * @param userId 用户 ID
     * @param phone  新手机号
     * @return 更新后的用户信息
     */
    UserVO updatePhone(Long userId, String phone);

    /**
     * 修改邮箱（调用方需先完成验证码校验）
     *
     * @param userId 用户 ID
     * @param email  新邮箱
     * @return 更新后的用户信息
     */
    UserVO updateEmail(Long userId, String email);

    /**
     * 查询用户邮箱（用于订单通知等场景）。
     *
     * @param userId 用户 ID
     * @return 邮箱地址，用户不存在时返回 null
     */
    String getEmail(Long userId);

    /**
     * 根据 ID 查询用户实体（模块间内部调用用）。
     *
     * @param userId 用户 ID
     * @return 用户实体，不存在返回 null
     */
    User getUserById(Long userId);

    /**
     * 批量查询用户显示名（nickname 优先，回退 username）。
     *
     * @param userIds 用户 ID 列表
     * @return Map<userId, displayName>，空列表返回 emptyMap
     */
    java.util.Map<Long, String> getUserDisplayNamesByIds(java.util.List<Long> userIds);

    /**
     * 批量查询用户名（username）。
     *
     * @param userIds 用户 ID 列表
     * @return Map<userId, username>，空列表返回 emptyMap
     */
    java.util.Map<Long, String> getUsernamesByIds(java.util.List<Long> userIds);

    /**
     * Phase 14：用户总数（封装 userMapper.selectCount(null)，消除跨模块 Mapper 依赖）。
     *
     * @return 用户总数
     */
    long countAll();

    /**
     * Phase 14：今日注册用户数（封装 userMapper.countTodayRegistered(today)）。
     *
     * @param today 当天日期
     * @return 注册数，可能为 null
     */
    Long countTodayRegistered(java.time.LocalDate today);

    /**
     * Phase 14：用户注册趋势（封装 userMapper.selectUserTrend(startDate, endDate)）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每行包含 dt(日期)、cnt(注册数)
     */
    java.util.List<java.util.Map<String, Object>> selectUserTrend(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * Phase 15：用户余额增加（封装 userMapper.update(null, wrapper)，消除 RechargeCardServiceImpl 跨模块 Mapper 依赖）。
     * <p>
     * 使用 {@code setSql} 直接执行 {@code balance = balance + amount}，避免覆盖更新。
     *
     * @param userId 用户 ID
     * @param amount 增加金额
     */
    void addBalance(Long userId, java.math.BigDecimal amount);

    /**
     * 扣减用户钱包余额（原子操作）。
     * @param userId 用户 ID
     * @param amount 扣减金额
     * @return 受影响行数（0 表示余额不足或用户不存在）
     */
    int deductBalance(Long userId, java.math.BigDecimal amount);

    /**
     * Phase 15：分页查询用户（封装 userMapper.selectPage(page, wrapper)，消除 AdminUserServiceImpl 跨模块 Mapper 依赖）。
     *
     * @param page    分页参数
     * @param wrapper 查询条件
     * @return 分页结果
     */
    com.baomidou.mybatisplus.core.metadata.IPage<User> selectUserPage(
            com.baomidou.mybatisplus.core.metadata.IPage<User> page,
            com.baomidou.mybatisplus.core.conditions.Wrapper<User> wrapper);

    /**
     * Phase 15：根据 ID 更新用户（封装 userMapper.updateById(user)，消除 AdminUserServiceImpl 跨模块 Mapper 依赖）。
     *
     * @param user 待更新用户实体（含主键）
     * @return 影响行数
     */
    int updateUserById(User user);
}