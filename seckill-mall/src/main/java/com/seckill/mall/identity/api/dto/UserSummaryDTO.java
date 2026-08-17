package com.seckill.mall.identity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户摘要 DTO，管理员用户列表/统计场景使用。
 *
 * <p>来源映射：User Entity → UserSummaryDTO
 * 相比 {@link UserSnapshot} 多包含 {@code createTime} 注册时间字段。
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像 URL */
    private String avatarUrl;

    /** 钱包余额 */
    private BigDecimal balance;

    /** 角色 */
    private String role;

    /** 状态 */
    private String status;

    /** 注册时间 */
    private LocalDateTime createTime;
}