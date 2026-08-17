package com.seckill.mall.security;

import com.seckill.mall.identity.infrastructure.entity.User;
import com.seckill.mall.identity.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SecurityUserDetailsService.java
 * 邮箱：nj651217@163.com
 */
@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            // 安全修复（L4）：使用统一错误信息"用户名或密码错误"，避免用户名枚举攻击
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return new SecurityUserDetails(user);
    }
}
