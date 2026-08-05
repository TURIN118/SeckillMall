package com.seckill.mall.security;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SecurityUtils.java
 * 邮箱：nj651217@163.com
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserMapper userMapper;

    /**
     * 获取当前用户 ID
     */
    public Long getCurrentUserId() {
        SecurityUserDetails details = getDetails();
        return details.getUserId();
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        SecurityUserDetails details = getDetails();
        return details.getUsername();
    }

    /**
     * 获取当前用户角色
     */
    public UserRole getCurrentUserRole() {
        SecurityUserDetails details = getDetails();
        return details.getRole();
    }

    /**
     * 获取当前用户昵称
     */
    public String getCurrentNickname() {
        SecurityUserDetails details = getDetails();
        return details.getNickname();
    }

    /**
     * 获取当前用户头像
     */
    public String getCurrentAvatar() {
        SecurityUserDetails details = getDetails();
        return details.getAvatar();
    }

    /**
     * 获取当前用户完整信息（需查 DB，谨慎使用）
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private SecurityUserDetails getDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUserDetails details)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return details;
    }
}
