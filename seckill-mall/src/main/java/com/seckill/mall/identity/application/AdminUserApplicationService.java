package com.seckill.mall.identity.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.identity.api.AdminUserApi;
import com.seckill.mall.identity.api.command.UpdateUserRoleCommand;
import com.seckill.mall.identity.api.command.UpdateUserStatusCommand;
import com.seckill.mall.identity.api.dto.LoginLogDTO;
import com.seckill.mall.identity.api.dto.UserSummaryDTO;
import com.seckill.mall.identity.api.query.LoginLogQuery;
import com.seckill.mall.identity.api.query.UserListQuery;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AdminUser 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link AdminUserApi}，内部委托给旧 {@link AdminUserService}，
 * 通过 {@link IdentityApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做参数转换、委托和结果转换。
 *
 * @author wnj
 * @since Phase I.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserApplicationService implements AdminUserApi {

    private final AdminUserService adminUserService;

    @Override
    public PageResult<UserSummaryDTO> listUsers(UserListQuery query) {
        return IdentityApiConverter.toSummaryDTOPage(
                adminUserService.getUserList(IdentityApiConverter.toUserListRequest(query)));
    }

    @Override
    public void updateUserStatus(UpdateUserStatusCommand command) {
        adminUserService.updateUserStatus(command.getUserId(),
                IdentityApiConverter.toUserStatus(command.getStatus()));
    }

    @Override
    public void updateUserRole(UpdateUserRoleCommand command) {
        adminUserService.updateUserRole(command.getUserId(),
                IdentityApiConverter.toUserRole(command.getRole()));
    }

    @Override
    public PageResult<LoginLogDTO> getUserLoginLogs(LoginLogQuery query) {
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        return IdentityApiConverter.toLoginLogDTOPage(
                adminUserService.getUserLoginLogs(query.getUserId(), pageNum, pageSize));
    }
}