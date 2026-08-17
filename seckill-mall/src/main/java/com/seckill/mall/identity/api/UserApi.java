package com.seckill.mall.identity.api;

import com.seckill.mall.identity.api.command.ChangePasswordCommand;
import com.seckill.mall.identity.api.command.UpdateEmailCommand;
import com.seckill.mall.identity.api.command.UpdatePhoneCommand;
import com.seckill.mall.identity.api.command.UpdateProfileCommand;
import com.seckill.mall.identity.api.command.UploadAvatarCommand;
import com.seckill.mall.identity.api.dto.UserSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Identity 模块用户业务能力 API。
 *
 * <p>对外暴露用户资料管理、密码/手机/邮箱修改、余额操作、统计查询、跨模块只读快照等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * @author wnj
 * @since Phase I.2
 */
public interface UserApi {

    /**
     * 根据 ID 查询用户快照（跨模块只读访问）。
     *
     * @param userId 用户 ID
     * @return 用户快照（不存在返回 null）
     */
    UserSnapshot getUserById(Long userId);

    /**
     * 获取当前登录用户的完整信息。
     *
     * @return 用户快照
     * @throws com.seckill.mall.exception.BusinessException {@code UNAUTHORIZED}
     */
    UserSnapshot getCurrentUser();

    /**
     * 更新当前登录用户的个人信息。
     *
     * @param command 更新资料命令
     * @return 更新后的用户信息
     * @throws com.seckill.mall.exception.BusinessException {@code UNAUTHORIZED}、{@code PARAM_ERROR}
     */
    UserSnapshot updateProfile(UpdateProfileCommand command);

    /**
     * 修改当前登录用户的密码。
     *
     * @param command 修改密码命令
     * @throws com.seckill.mall.exception.BusinessException {@code UNAUTHORIZED}、{@code PASSWORD_INCORRECT}、{@code PARAM_ERROR}
     */
    void changePassword(ChangePasswordCommand command);

    /**
     * 修改用户手机号（调用方需先完成验证码校验）。
     *
     * @param command 修改手机号命令
     * @return 更新后的用户信息
     * @throws com.seckill.mall.exception.BusinessException {@code USER_NOT_FOUND}、{@code PARAM_ERROR}
     */
    UserSnapshot updateUserPhone(UpdatePhoneCommand command);

    /**
     * 修改用户邮箱（调用方需先完成验证码校验）。
     *
     * @param command 修改邮箱命令
     * @return 更新后的用户信息
     * @throws com.seckill.mall.exception.BusinessException {@code USER_NOT_FOUND}、{@code PARAM_ERROR}
     */
    UserSnapshot updateUserEmail(UpdateEmailCommand command);

    /**
     * 查询用户邮箱（用于订单通知等场景）。
     *
     * @param userId 用户 ID
     * @return 邮箱地址（用户不存在时返回 null）
     */
    String getUserEmail(Long userId);

    /**
     * 批量查询用户显示名（nickname 优先，回退 username）。
     *
     * @param userIds 用户 ID 列表
     * @return userId → displayName 映射（空列表返回 emptyMap）
     */
    Map<Long, String> getUserDisplayNamesByIds(List<Long> userIds);

    /**
     * 批量查询用户名。
     *
     * @param userIds 用户 ID 列表
     * @return userId → username 映射（空列表返回 emptyMap）
     */
    Map<Long, String> getUsernamesByIds(List<Long> userIds);

    /**
     * 查询用户总数。
     *
     * @return 用户总数
     */
    long countAll();

    /**
     * 查询指定日期的注册用户数。
     *
     * @param today 查询日期
     * @return 注册数（可能为 null）
     */
    Long countTodayRegistered(LocalDate today);

    /**
     * 查询日期范围内的用户注册趋势。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每行包含 dt(日期)、cnt(注册数)
     */
    List<Map<String, Object>> selectUserTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 用户钱包余额增加（原子操作）。
     *
     * @param userId 用户 ID
     * @param amount 增加金额（>0）
     * @throws com.seckill.mall.exception.BusinessException {@code USER_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void addBalance(Long userId, BigDecimal amount);

    /**
     * 扣减用户钱包余额（原子操作）。
     *
     * @param userId 用户 ID
     * @param amount 扣减金额（>0）
     * @return 受影响行数（0 表示余额不足或用户不存在）
     */
    int deductBalance(Long userId, BigDecimal amount);

    /**
     * 上传当前登录用户的头像，并持久化头像 URL。
     *
     * @param command 上传头像命令
     * @return 包含 {@code avatar} → URL 的映射
     * @throws com.seckill.mall.exception.BusinessException {@code UNAUTHORIZED}、{@code FILE_TOO_LARGE}、{@code FILE_TYPE_NOT_SUPPORTED}
     */
    Map<String, String> uploadAvatar(UploadAvatarCommand command);
}