package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.UserListRequest;
import com.seckill.mall.entity.LoginLog;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.LoginResult;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.entity.enums.UserStatus;
import com.seckill.mall.mapper.LoginLogMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.AdminUserService;
import com.seckill.mall.security.TokenVersionService;
import com.seckill.mall.security.UserStatusCacheService;
import com.seckill.mall.vo.LoginLogVO;
import com.seckill.mall.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminUserServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final TokenVersionService tokenVersionService;
    private final UserStatusCacheService userStatusCacheService;

    @Override
    public PageResult<UserVO> getUserList(UserListRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : req.getPageSize();

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("id", "username", "phone", "email", "nickname", "avatar_url",
                "role", "status", "create_time");
        if (req.getRole() != null && !req.getRole().isBlank()) {
            wrapper.eq("role", req.getRole());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            wrapper.eq("status", req.getStatus());
        }
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String kw = req.getKeyword().trim();
            wrapper.and(w -> w.like("username", kw).or().like("phone", kw).or().like("nickname", kw));
        }
        wrapper.orderByDesc("create_time");

        IPage<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UserVO> list = page.getRecords() == null ? Collections.emptyList()
                : page.getRecords().stream().map(this::toUserVO).toList();
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = loadUser(userId);
        User update = new User();
        update.setId(user.getId());
        update.setStatus(status);
        userMapper.updateById(update);
        // 禁用/锁定用户后递增 Token 版本号（踢下所有设备）
        tokenVersionService.incrementVersion(userId);
        // 刷新用户状态缓存
        userStatusCacheService.refreshUserAuth(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long userId, UserRole role) {
        User user = loadUser(userId);
        User update = new User();
        update.setId(user.getId());
        update.setRole(role);
        userMapper.updateById(update);
        // 修改角色后刷新用户状态缓存
        userStatusCacheService.refreshUserAuth(userId);
    }

    @Override
    public PageResult<LoginLogVO> getUserLoginLogs(Long userId, Integer pageNum, Integer pageSize) {
        loadUser(userId);

        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;

        QueryWrapper<LoginLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        IPage<LoginLog> page = loginLogMapper.selectPage(new Page<>(pn, ps), wrapper);
        List<LoginLogVO> list = page.getRecords() == null ? Collections.emptyList()
                : page.getRecords().stream().map(this::toLoginLogVO).toList();
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private User loadUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

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

    private LoginLogVO toLoginLogVO(LoginLog loginLog) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(loginLog.getId());
        vo.setUserId(loginLog.getUserId());
        vo.setLoginIp(loginLog.getLoginIp());
        vo.setLoginLocation(loginLog.getLoginLocation());
        vo.setUserAgent(loginLog.getUserAgent());
        LoginResult result = loginLog.getLoginResult();
        vo.setLoginResult(result == null ? null : result.getCode());
        vo.setFailReason(loginLog.getFailReason());
        vo.setLoginTime(loginLog.getCreateTime());
        return vo;
    }
}
