package com.seckill.mall.security;

import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.entity.enums.UserStatus;
import com.seckill.mall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户状态 Redis 缓存服务。
 * 缓存用户状态/角色信息，避免每次鉴权请求都查数据库。
 * TTL 60 秒，配合主动失效策略实现秒级一致性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusCacheService {

    private static final String KEY_PREFIX = "user:auth:";
    private static final long CACHE_TTL_SECONDS = 60L;

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;

    /**
     * 用户缓存数据载体
     */
    public record UserAuthCache(UserStatus status, UserRole role, String username) {}

    /**
     * 获取用户缓存。命中返回缓存数据，未命中返回 null。
     */
    public UserAuthCache getUserAuth(Long userId) {
        try {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(KEY_PREFIX + userId);
            if (entries.isEmpty()) {
                return null;
            }
            String statusStr = (String) entries.get("status");
            String roleStr = (String) entries.get("role");
            String username = (String) entries.get("username");
            if (statusStr == null || roleStr == null) {
                return null;
            }
            return new UserAuthCache(
                    UserStatus.valueOf(statusStr),
                    UserRole.fromCode(roleStr),
                    username
            );
        } catch (Exception e) {
            log.warn("读取用户缓存失败，降级查 DB: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 写入用户缓存（登录成功 / DB 查询后回填）
     */
    public void putUserAuth(Long userId, User user) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("status", user.getStatus().name());
            data.put("role", user.getRole().getCode());
            data.put("username", user.getUsername());
            stringRedisTemplate.opsForHash().putAll(KEY_PREFIX + userId, data);
            stringRedisTemplate.expire(KEY_PREFIX + userId, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入用户缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 使缓存失效（状态/角色变更时调用）
     */
    public void invalidateUserAuth(Long userId) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("删除用户缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 刷新缓存（删除旧缓存后立即从 DB 重新加载）
     * 用于管理员修改用户状态后，确保下一个请求能立即拿到最新数据
     */
    public void refreshUserAuth(Long userId) {
        invalidateUserAuth(userId);
        User user = userMapper.selectById(userId);
        if (user != null) {
            putUserAuth(userId, user);
        }
    }
}