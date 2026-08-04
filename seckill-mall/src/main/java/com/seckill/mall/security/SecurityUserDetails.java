package com.seckill.mall.security;

import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SecurityUserDetails.java
 * 邮箱：nj651217@163.com
 */
@Getter
public class SecurityUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final UserRole role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.enabled = user.getStatus() != null && user.getStatus().name().equals("ACTIVE");
        // Spring Security 角色权限需以 ROLE_ 前缀标识
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode())
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        // 安全修复（M9）：账户过期与启用状态联动，禁用账户视为过期
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 安全修复（M9）：账户锁定状态与 enabled 字段联动，禁用账户视为已锁定
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 安全修复（M9）：凭证过期与 enabled 字段联动，禁用账户视为凭证过期
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
