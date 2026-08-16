package com.seckill.mall.service;

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
}