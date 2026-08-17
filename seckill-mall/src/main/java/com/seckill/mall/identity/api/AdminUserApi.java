package com.seckill.mall.identity.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.identity.api.command.UpdateUserStatusCommand;
import com.seckill.mall.identity.api.command.UpdateUserRoleCommand;
import com.seckill.mall.identity.api.dto.LoginLogDTO;
import com.seckill.mall.identity.api.dto.UserSummaryDTO;
import com.seckill.mall.identity.api.query.LoginLogQuery;
import com.seckill.mall.identity.api.query.UserListQuery;

/**
 * Identity 模块管理员用户管理 API。
 *
 * <p>对外暴露管理员用户管理（列表/状态/角色/登录日志）契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 PageResult/DTO，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * @author wnj
 * @since Phase I.2
 */
public interface AdminUserApi {

    /**
     * 管理员查询用户列表（支持按角色/状态筛选 + 分页）。
     *
     * @param query 用户列表查询条件
     * @return 分页结果
     * @throws com.seckill.mall.exception.BusinessException {@code PARAM_ERROR}
     */
    PageResult<UserSummaryDTO> listUsers(UserListQuery query);

    /**
     * 启用/禁用/锁定用户（同步更新 security 缓存中的用户状态）。
     *
     * @param command 更新用户状态命令
     * @throws com.seckill.mall.exception.BusinessException {@code USER_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void updateUserStatus(UpdateUserStatusCommand command);

    /**
     * 修改用户角色（BUYER/SELLER/ADMIN）。
     *
     * @param command 更新用户角色命令
     * @throws com.seckill.mall.exception.BusinessException {@code USER_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void updateUserRole(UpdateUserRoleCommand command);

    /**
     * 查询指定用户的登录日志（分页）。
     *
     * @param query 登录日志查询条件
     * @return 分页结果
     */
    PageResult<LoginLogDTO> getUserLoginLogs(LoginLogQuery query);
}