package com.seckill.mall.service;

import com.seckill.mall.entity.User;
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
}