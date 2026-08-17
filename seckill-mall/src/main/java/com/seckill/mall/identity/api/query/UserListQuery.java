package com.seckill.mall.identity.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户列表查询条件（管理员场景）。
 *
 * <p>支持按角色/状态筛选 + 用户名/昵称关键字模糊匹配，分页查询。
 *
 * <p>原方法：{@code AdminUserService.getUserList(UserListRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListQuery {

    /** 页码（默认 1） */
    private Integer pageNum;

    /** 每页大小（默认 10，上限 50） */
    private Integer pageSize;

    /** 角色筛选（BUYER/SELLER/ADMIN） */
    private String role;

    /** 状态筛选（ACTIVE/LOCKED/DISABLED） */
    private String status;

    /** 用户名/昵称关键字模糊匹配 */
    private String keyword;
}