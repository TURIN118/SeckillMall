package com.seckill.mall.controller;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.EmailUpdateRequest;
import com.seckill.mall.dto.PhoneUpdateRequest;
import com.seckill.mall.entity.User;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.VerificationCodeService;
import com.seckill.mall.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人信息 Controller
 * <p>
 * 前缀 {@code /api/v1/users}，需登录（BUYER/ADMIN）。
 * 提供修改手机号/邮箱接口，均需先校验验证码。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "用户个人信息", description = "修改手机号/邮箱（需验证码校验）")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class UserController {

    private final UserMapper userMapper;
    private final VerificationCodeService verificationCodeService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "修改手机号（需验证码校验）")
    @PutMapping("/profile/phone")
    public Result<UserVO> updatePhone(@Valid @RequestBody PhoneUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        // 先校验验证码（target 为新手机号）
        if (!verificationCodeService.verifyCode(req.getPhone(), req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        // 校验手机号唯一性
        User existing = userMapper.findByPhone(req.getPhone());
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
        // 更新手机号
        User update = new User();
        update.setId(userId);
        update.setPhone(req.getPhone());
        userMapper.updateById(update);
        // 返回最新用户信息
        User latest = userMapper.selectById(userId);
        return Result.success("手机号修改成功", toUserVO(latest));
    }

    @Operation(summary = "修改邮箱（需验证码校验）")
    @PutMapping("/profile/email")
    public Result<UserVO> updateEmail(@Valid @RequestBody EmailUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        // 先校验验证码（target 为新邮箱）
        if (!verificationCodeService.verifyCode(req.getEmail(), req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        // 校验邮箱唯一性
        User existing = userMapper.findByEmail(req.getEmail());
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        // 更新邮箱
        User update = new User();
        update.setId(userId);
        update.setEmail(req.getEmail());
        userMapper.updateById(update);
        // 返回最新用户信息
        User latest = userMapper.selectById(userId);
        return Result.success("邮箱修改成功", toUserVO(latest));
    }

    /** Entity → VO */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatarUrl());
        vo.setRole(user.getRole() == null ? null : user.getRole().getCode());
        vo.setStatus(user.getStatus() == null ? null : user.getStatus().getCode());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}