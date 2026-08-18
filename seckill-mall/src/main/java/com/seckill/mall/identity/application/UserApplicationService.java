package com.seckill.mall.identity.application;

import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.identity.api.command.ChangePasswordCommand;
import com.seckill.mall.identity.api.command.UpdateEmailCommand;
import com.seckill.mall.identity.api.command.UpdatePhoneCommand;
import com.seckill.mall.identity.api.command.UpdateProfileCommand;
import com.seckill.mall.identity.api.command.UploadAvatarCommand;
import com.seckill.mall.identity.api.dto.UserSnapshot;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.service.AuthService;
import com.seckill.mall.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * User 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link UserApi}，内部委托给旧 {@link UserService} 与 {@link AuthService}，
 * 通过 {@link IdentityApiConverter} 做 VO/Entity ↔ DTO/Snapshot 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command/Query 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO/Entity 转换为 API 层 DTO/Snapshot</li>
 * </ol>
 *
 * <p>职责划分：
 * <ul>
 *     <li>{@link UserService}：手机/邮箱修改、邮箱查询、批量查询、统计、余额操作</li>
 *     <li>{@link AuthService}：当前用户查询、资料更新、密码修改、头像上传</li>
 * </ul>
 *
 * @author wnj
 * @since Phase I.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService implements UserApi {

    private final UserService userService;
    private final AuthService authService;

    @Override
    public UserSnapshot getUserById(Long userId) {
        return IdentityApiConverter.toSnapshot(userService.getUserById(userId));
    }

    @Override
    public UserSnapshot getCurrentUser() {
        return IdentityApiConverter.toSnapshotFromVO(authService.getMe());
    }

    @Override
    public UserSnapshot updateProfile(UpdateProfileCommand command) {
        return IdentityApiConverter.toSnapshotFromVO(
                authService.updateProfile(IdentityApiConverter.toProfileUpdateRequest(command)));
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        authService.changePassword(IdentityApiConverter.toChangePasswordRequest(command));
    }

    @Override
    public UserSnapshot updateUserPhone(UpdatePhoneCommand command) {
        return IdentityApiConverter.toSnapshotFromVO(
                userService.updatePhone(command.getUserId(), command.getPhone()));
    }

    @Override
    public UserSnapshot updateUserEmail(UpdateEmailCommand command) {
        return IdentityApiConverter.toSnapshotFromVO(
                userService.updateEmail(command.getUserId(), command.getEmail()));
    }

    @Override
    public String getUserEmail(Long userId) {
        return userService.getEmail(userId);
    }

    @Override
    public Map<Long, String> getUserDisplayNamesByIds(List<Long> userIds) {
        return userService.getUserDisplayNamesByIds(userIds);
    }

    @Override
    public Map<Long, String> getUsernamesByIds(List<Long> userIds) {
        return userService.getUsernamesByIds(userIds);
    }

    @Override
    public long countAll() {
        return userService.countAll();
    }

    @Override
    public Long countTodayRegistered(LocalDate today) {
        return userService.countTodayRegistered(today);
    }

    @Override
    public List<Map<String, Object>> selectUserTrend(LocalDate startDate, LocalDate endDate) {
        return userService.selectUserTrend(startDate, endDate);
    }

    @Override
    public void addBalance(Long userId, BigDecimal amount) {
        userService.addBalance(userId, amount);
    }

    @Override
    public int deductBalance(Long userId, BigDecimal amount) {
        return userService.deductBalance(userId, amount);
    }

    @Override
    public Map<String, String> uploadAvatar(UploadAvatarCommand command) {
        return authService.uploadAvatar(command.getFile());
    }
}