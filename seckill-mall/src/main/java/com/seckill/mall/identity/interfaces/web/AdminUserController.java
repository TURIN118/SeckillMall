package com.seckill.mall.identity.interfaces.web;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.UserListRequest;
import com.seckill.mall.dto.UserRoleUpdateRequest;
import com.seckill.mall.dto.UserStatusUpdateRequest;
import com.seckill.mall.identity.api.AdminUserApi;
import com.seckill.mall.identity.api.command.UpdateUserRoleCommand;
import com.seckill.mall.identity.api.command.UpdateUserStatusCommand;
import com.seckill.mall.identity.api.dto.LoginLogDTO;
import com.seckill.mall.identity.api.dto.UserSummaryDTO;
import com.seckill.mall.identity.api.query.LoginLogQuery;
import com.seckill.mall.identity.api.query.UserListQuery;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.vo.LoginLogVO;
import com.seckill.mall.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台用户管理控制器（Phase I.4-C 已切换到 {@link AdminUserApi}）。
 *
 * <p>Strangler Pattern：注入 {@link AdminUserApi} 替代旧 {@code AdminUserService}，
 * 通过 {@link IdentityApiConverter} 做旧 Request → Query/Command、DTO → VO 转换，
 * 保持前端入参/出参结构不变。
 *
 * <p>创建人：@author WNJ
 */
@Tag(name = "后台用户管理", description = "用户列表/启禁用/改角色")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserApi adminUserApi;

    @Operation(summary = "用户列表（分页+角色/状态筛选）")
    @GetMapping
    public Result<PageResult<UserVO>> list(@Valid UserListRequest req) {
        UserListQuery query = IdentityApiConverter.toUserListQuery(req);
        PageResult<UserSummaryDTO> dtoPage = adminUserApi.listUsers(query);
        return Result.success(IdentityApiConverter.toUserVOPage(dtoPage));
    }

    @Operation(summary = "启用/禁用用户")
    @OperationLog(module = "USER", action = "UPDATE_STATUS", targetIdSpEL = "#userId", targetType = "USER")
    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(@PathVariable Long userId,
                                     @Valid @RequestBody UserStatusUpdateRequest req) {
        UpdateUserStatusCommand command = UpdateUserStatusCommand.builder()
                .userId(userId)
                .status(req.getStatus())
                .build();
        adminUserApi.updateUserStatus(command);
        return Result.success();
    }

    @Operation(summary = "修改用户角色")
    @OperationLog(module = "USER", action = "UPDATE_ROLE", targetIdSpEL = "#userId", targetType = "USER")
    @PutMapping("/{userId}/role")
    public Result<Void> updateRole(@PathVariable Long userId,
                                   @Valid @RequestBody UserRoleUpdateRequest req) {
        UpdateUserRoleCommand command = UpdateUserRoleCommand.builder()
                .userId(userId)
                .role(req.getRole())
                .build();
        adminUserApi.updateUserRole(command);
        return Result.success();
    }

    @Operation(summary = "用户登录日志（分页）")
    @GetMapping("/{userId}/logs")
    public Result<PageResult<LoginLogVO>> loginLogs(@PathVariable Long userId,
                                                    @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                                    @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        LoginLogQuery query = LoginLogQuery.builder()
                .userId(userId)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        PageResult<LoginLogDTO> dtoPage = adminUserApi.getUserLoginLogs(query);
        return Result.success(IdentityApiConverter.toLoginLogVOPage(dtoPage));
    }
}
