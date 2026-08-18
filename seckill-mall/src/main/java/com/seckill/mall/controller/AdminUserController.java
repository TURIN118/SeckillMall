package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.UserListRequest;
import com.seckill.mall.dto.UserRoleUpdateRequest;
import com.seckill.mall.dto.UserStatusUpdateRequest;
import com.seckill.mall.identity.domain.UserRole;
import com.seckill.mall.identity.domain.UserStatus;
import com.seckill.mall.service.AdminUserService;
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
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminUserController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "后台用户管理", description = "用户列表/启禁用/改角色")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户列表（分页+角色/状态筛选）")
    @GetMapping
    public Result<PageResult<UserVO>> list(@Valid UserListRequest req) {
        return Result.success(adminUserService.getUserList(req));
    }

    @Operation(summary = "启用/禁用用户")
    @OperationLog(module = "USER", action = "UPDATE_STATUS", targetIdSpEL = "#userId", targetType = "USER")
    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(@PathVariable Long userId,
                                     @Valid @RequestBody UserStatusUpdateRequest req) {
        adminUserService.updateUserStatus(userId, UserStatus.fromCode(req.getStatus()));
        return Result.success();
    }

    @Operation(summary = "修改用户角色")
    @OperationLog(module = "USER", action = "UPDATE_ROLE", targetIdSpEL = "#userId", targetType = "USER")
    @PutMapping("/{userId}/role")
    public Result<Void> updateRole(@PathVariable Long userId,
                                   @Valid @RequestBody UserRoleUpdateRequest req) {
        adminUserService.updateUserRole(userId, UserRole.fromCode(req.getRole()));
        return Result.success();
    }

    @Operation(summary = "用户登录日志（分页）")
    @GetMapping("/{userId}/logs")
    public Result<PageResult<LoginLogVO>> loginLogs(@PathVariable Long userId,
                                                    @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                                    @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(adminUserService.getUserLoginLogs(userId, pageNum, pageSize));
    }
}
