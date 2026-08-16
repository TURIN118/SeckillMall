package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.User;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.UserService;
import com.seckill.mall.utils.DataMaskUtil;
import com.seckill.mall.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * 用户个人信息服务实现
 * <p>
 * M-D2 修复：从 {@code UserController} 下沉而来，封装对 {@code UserMapper} 的访问，
 * Controller 不再直接依赖 Mapper。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updatePhone(Long userId, String phone) {
        // 校验手机号唯一性
        User existing = userMapper.findByPhone(phone);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
        // 更新手机号
        User update = new User();
        update.setId(userId);
        update.setPhone(phone);
        userMapper.updateById(update);
        log.info("用户手机号修改成功，userId={}", userId);
        // 返回最新用户信息
        User latest = userMapper.selectById(userId);
        return toUserVO(latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateEmail(Long userId, String email) {
        // 校验邮箱唯一性
        User existing = userMapper.findByEmail(email);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        // 更新邮箱
        User update = new User();
        update.setId(userId);
        update.setEmail(email);
        userMapper.updateById(update);
        log.info("用户邮箱修改成功，userId={}", userId);
        // 返回最新用户信息
        User latest = userMapper.selectById(userId);
        return toUserVO(latest);
    }

    @Override
    public String getEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getEmail();
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public Map<Long, String> getUserDisplayNamesByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getNickname() != null && !u.getNickname().isBlank()
                                ? u.getNickname() : u.getUsername(),
                        (a, b) -> a));
    }

    @Override
    public Map<Long, String> getUsernamesByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }

    /**
     * Phase 14：用户总数，封装 userMapper.selectCount(null)。
     */
    @Override
    public long countAll() {
        return userMapper.selectCount(null);
    }

    /**
     * Phase 14：今日注册用户数，封装 userMapper.countTodayRegistered(today)。
     */
    @Override
    public Long countTodayRegistered(LocalDate today) {
        return userMapper.countTodayRegistered(today);
    }

    /**
     * Phase 14：用户注册趋势，封装 userMapper.selectUserTrend(startDate, endDate)。
     */
    @Override
    public List<Map<String, Object>> selectUserTrend(LocalDate startDate, LocalDate endDate) {
        return userMapper.selectUserTrend(startDate, endDate);
    }

    /**
     * Phase 15：用户余额增加，封装 userMapper.update(null, wrapper)。
     * <p>
     * 消除 RechargeCardServiceImpl 对 UserMapper 的跨模块依赖。
     */
    @Override
    public void addBalance(Long userId, java.math.BigDecimal amount) {
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("balance = balance + " + amount.toPlainString()));
    }

    @Override
    public int deductBalance(Long userId, java.math.BigDecimal amount) {
        return userMapper.deductBalance(userId, amount);
    }

    /**
     * Phase 15：分页查询用户，封装 userMapper.selectPage(page, wrapper)。
     * <p>
     * 消除 AdminUserServiceImpl 对 UserMapper 的跨模块依赖。
     */
    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<User> selectUserPage(
            com.baomidou.mybatisplus.core.metadata.IPage<User> page,
            com.baomidou.mybatisplus.core.conditions.Wrapper<User> wrapper) {
        return userMapper.selectPage(page, wrapper);
    }

    /**
     * Phase 15：根据 ID 更新用户，封装 userMapper.updateById(user)。
     * <p>
     * 消除 AdminUserServiceImpl 对 UserMapper 的跨模块依赖。
     */
    @Override
    public int updateUserById(User user) {
        return userMapper.updateById(user);
    }

    /** Entity → VO */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        // M31 安全说明：phone/email 脱敏，复用 DataMaskUtil 统一实现
        vo.setPhone(DataMaskUtil.maskPhone(user.getPhone()));
        vo.setEmail(DataMaskUtil.maskEmail(user.getEmail()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatarUrl());
        vo.setRole(user.getRole() == null ? null : user.getRole().getCode());
        vo.setStatus(user.getStatus() == null ? null : user.getStatus().getCode());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

}