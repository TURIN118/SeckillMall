package com.seckill.mall.security;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SecurityUtils {

    private static UserMapper userMapper;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        SecurityUtils.userMapper = userMapper;
    }

    public static Long getCurrentUserId() {
        SecurityUserDetails details = getDetails();
        return details.getUserId();
    }

    public static String getCurrentUsername() {
        SecurityUserDetails details = getDetails();
        return details.getUsername();
    }

    public static UserRole getCurrentUserRole() {
        SecurityUserDetails details = getDetails();
        return details.getRole();
    }

    public static User getCurrentUser() {
        Long userId = getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private static SecurityUserDetails getDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUserDetails details)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return details;
    }
}
