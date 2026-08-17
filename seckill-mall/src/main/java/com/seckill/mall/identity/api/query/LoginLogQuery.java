package com.seckill.mall.identity.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录日志查询条件。
 *
 * <p>查询指定用户的登录日志（分页）。
 *
 * <p>原方法：{@code AdminUserService.getUserLoginLogs(Long, Integer, Integer)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogQuery {

    /** 用户 ID（必填） */
    private Long userId;

    /** 页码（默认 1） */
    private Integer pageNum;

    /** 每页大小（默认 10） */
    private Integer pageSize;
}