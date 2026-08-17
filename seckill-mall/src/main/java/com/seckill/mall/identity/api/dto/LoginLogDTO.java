package com.seckill.mall.identity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录日志 DTO，管理员查询用户登录日志使用。
 *
 * <p>来源映射：LoginLog Entity → LoginLogDTO
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogDTO {

    /** 日志 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 登录 IP */
    private String ip;

    /** User-Agent */
    private String userAgent;

    /** 登录结果（SUCCESS/FAILURE） */
    private String loginResult;

    /** 登录时间 */
    private LocalDateTime loginTime;
}