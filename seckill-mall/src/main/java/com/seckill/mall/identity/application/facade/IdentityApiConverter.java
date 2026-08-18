package com.seckill.mall.identity.application.facade;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.ForgotPasswordResetRequest;
import com.seckill.mall.dto.ForgotPasswordSendRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.ProfileUpdateRequest;
import com.seckill.mall.dto.RefreshTokenRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.dto.UserListRequest;
import com.seckill.mall.identity.api.command.AddFavoriteCommand;
import com.seckill.mall.identity.api.command.ChangePasswordCommand;
import com.seckill.mall.identity.api.command.LoginCommand;
import com.seckill.mall.identity.api.command.RefreshTokenCommand;
import com.seckill.mall.identity.api.command.RegisterCommand;
import com.seckill.mall.identity.api.command.RemoveFavoriteCommand;
import com.seckill.mall.identity.api.command.ResetPasswordCommand;
import com.seckill.mall.identity.api.command.SaveAddressCommand;
import com.seckill.mall.identity.api.command.SendCodeCommand;
import com.seckill.mall.identity.api.command.UpdateAddressCommand;
import com.seckill.mall.identity.api.command.UpdateEmailCommand;
import com.seckill.mall.identity.api.command.UpdatePhoneCommand;
import com.seckill.mall.identity.api.command.UpdateProfileCommand;
import com.seckill.mall.identity.api.command.UpdateUserRoleCommand;
import com.seckill.mall.identity.api.command.UpdateUserStatusCommand;
import com.seckill.mall.identity.api.command.UploadAvatarCommand;
import com.seckill.mall.identity.api.dto.AddressDTO;
import com.seckill.mall.identity.api.dto.FavoriteItemDTO;
import com.seckill.mall.identity.api.dto.LoginLogDTO;
import com.seckill.mall.identity.api.dto.UserSnapshot;
import com.seckill.mall.identity.api.dto.UserSummaryDTO;
import com.seckill.mall.identity.api.query.LoginLogQuery;
import com.seckill.mall.identity.api.query.UserListQuery;
import com.seckill.mall.identity.api.result.CaptchaResult;
import com.seckill.mall.identity.api.result.LoginResult;
import com.seckill.mall.identity.api.result.TokenResult;

import com.seckill.mall.identity.domain.UserRole;
import com.seckill.mall.identity.domain.UserStatus;
import com.seckill.mall.identity.infrastructure.entity.LoginLog;
import com.seckill.mall.identity.infrastructure.entity.User;
import com.seckill.mall.identity.infrastructure.entity.UserAddress;
import com.seckill.mall.vo.CaptchaVO;
import com.seckill.mall.vo.FavoriteItemVO;
import com.seckill.mall.vo.LoginLogVO;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UserAddressVO;
import com.seckill.mall.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Identity API 转换辅助类。
 *
 * <p>集中存放旧 VO/Entity 与新 API 层 DTO/Result/Snapshot 之间的转换方法，
 * 供 ApplicationService 调用。所有方法均为无状态静态方法，
 * 标注 {@code @Component} 仅为便于未来扩展为 Bean 注入方式。
 *
 * <p>转换原则：
 * <ul>
 *     <li>VO → Result/DTO：提取核心字段，丢弃前端展示专用字段</li>
 *     <li>Entity → Snapshot：仅提取跨模块传递所需字段，避免暴露 Entity</li>
 *     <li>Command → Request：API 层 Command → 旧 DTO Request，字段一一对应</li>
 *     <li>Query → Request：API 层 Query → 旧 DTO Request，字段一一对应</li>
 * </ul>
 *
 * @author wnj
 * @since Phase I.4-A
 */
@Slf4j
@Component
public class IdentityApiConverter {

    // ============================================================
    // User Entity → UserSnapshot 转换（跨模块只读快照）
    // ============================================================

    /**
     * 将 {@link User} Entity 转换为 {@link UserSnapshot}。
     *
     * <p>仅提取跨模块传递所需字段，避免暴露 Entity。
     * {@code role}/{@code status} 枚举转为 String 名称。
     *
     * @param entity 用户 Entity
     * @return 用户快照
     */
    public static UserSnapshot toSnapshot(User entity) {
        if (entity == null) {
            return null;
        }
        return UserSnapshot.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .nickname(entity.getNickname())
                .avatarUrl(entity.getAvatarUrl())
                .balance(entity.getBalance())
                .role(entity.getRole() != null ? entity.getRole().getCode() : null)
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .build();
    }

    /**
     * 将 {@link User} Entity 转换为 {@link UserSummaryDTO}（含 createTime）。
     *
     * @param entity 用户 Entity
     * @return 用户摘要 DTO
     */
    public static UserSummaryDTO toSummaryDTO(User entity) {
        if (entity == null) {
            return null;
        }
        return UserSummaryDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nickname(entity.getNickname())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .avatarUrl(entity.getAvatarUrl())
                .balance(entity.getBalance())
                .role(entity.getRole() != null ? entity.getRole().getCode() : null)
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .createTime(entity.getCreateTime())
                .build();
    }

    // ============================================================
    // UserVO → UserSnapshot / UserSummaryDTO 转换
    // ============================================================

    /**
     * 将 {@link UserVO} 转换为 {@link UserSnapshot}。
     *
     * <p>旧 Service 返回 UserVO，需转换为 API 层 UserSnapshot。
     * 注意：UserVO 无 balance 字段，转换后 balance 为 null。
     *
     * @param vo 用户 VO
     * @return 用户快照
     */
    public static UserSnapshot toSnapshotFromVO(UserVO vo) {
        if (vo == null) {
            return null;
        }
        return UserSnapshot.builder()
                .id(vo.getId())
                .username(vo.getUsername())
                .phone(vo.getPhone())
                .email(vo.getEmail())
                .nickname(vo.getNickname())
                .avatarUrl(vo.getAvatar())
                .role(vo.getRole())
                .status(vo.getStatus())
                .build();
    }

    /**
     * 将 {@link UserVO} 转换为 {@link UserSummaryDTO}。
     *
     * @param vo 用户 VO
     * @return 用户摘要 DTO
     */
    public static UserSummaryDTO toSummaryDTOFromVO(UserVO vo) {
        if (vo == null) {
            return null;
        }
        return UserSummaryDTO.builder()
                .id(vo.getId())
                .username(vo.getUsername())
                .nickname(vo.getNickname())
                .phone(vo.getPhone())
                .email(vo.getEmail())
                .avatarUrl(vo.getAvatar())
                .role(vo.getRole())
                .status(vo.getStatus())
                .createTime(vo.getCreateTime())
                .build();
    }

    // ============================================================
    // UserAddress Entity → AddressDTO 转换
    // ============================================================

    /**
     * 将 {@link UserAddress} Entity 转换为 {@link AddressDTO}。
     *
     * @param entity 地址 Entity
     * @return 地址 DTO
     */
    public static AddressDTO toAddressDTO(UserAddress entity) {
        if (entity == null) {
            return null;
        }
        return AddressDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .receiverName(entity.getReceiverName())
                .receiverPhone(entity.getReceiverPhone())
                .province(entity.getProvince())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .detailAddress(entity.getDetailAddress())
                .isDefault(entity.getIsDefault())
                .build();
    }

    // ============================================================
    // UserAddressVO → AddressDTO 转换
    // ============================================================

    /**
     * 将 {@link UserAddressVO} 转换为 {@link AddressDTO}。
     *
     * @param vo 地址 VO
     * @return 地址 DTO
     */
    public static AddressDTO toAddressDTOFromVO(UserAddressVO vo) {
        if (vo == null) {
            return null;
        }
        return AddressDTO.builder()
                .id(vo.getId())
                .userId(vo.getUserId())
                .receiverName(vo.getReceiverName())
                .receiverPhone(vo.getReceiverPhone())
                .province(vo.getProvince())
                .city(vo.getCity())
                .district(vo.getDistrict())
                .detailAddress(vo.getDetailAddress())
                .isDefault(vo.getIsDefault())
                .build();
    }

    /**
     * 将 {@link UserAddressVO} 列表转换为 {@link AddressDTO} 列表。
     *
     * @param voList 地址 VO 列表
     * @return 地址 DTO 列表
     */
    public static List<AddressDTO> toAddressDTOListFromVO(List<UserAddressVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(IdentityApiConverter::toAddressDTOFromVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // FavoriteItemVO → FavoriteItemDTO 转换
    // ============================================================

    /**
     * 将 {@link FavoriteItemVO} 转换为 {@link FavoriteItemDTO}。
     *
     * <p>旧 Service 返回 FavoriteItemVO（不含 userId/createTime），转换时这些字段为 null。
     *
     * @param vo 收藏项 VO
     * @return 收藏项 DTO
     */
    public static FavoriteItemDTO toFavoriteItemDTOFromVO(FavoriteItemVO vo) {
        if (vo == null) {
            return null;
        }
        return FavoriteItemDTO.builder()
                .id(vo.getId())
                .productId(vo.getProductId())
                .productName(vo.getProductName())
                .productMainImage(vo.getMainImage())
                .productPrice(vo.getOriginalPrice())
                .productStatus(vo.getProductStatus())
                .build();
    }

    /**
     * 将 {@link FavoriteItemVO} 列表转换为 {@link FavoriteItemDTO} 列表。
     *
     * @param voList 收藏项 VO 列表
     * @return 收藏项 DTO 列表
     */
    public static List<FavoriteItemDTO> toFavoriteItemDTOListFromVO(List<FavoriteItemVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(IdentityApiConverter::toFavoriteItemDTOFromVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // LoginLogVO → LoginLogDTO 转换
    // ============================================================

    /**
     * 将 {@link LoginLogVO} 转换为 {@link LoginLogDTO}。
     *
     * @param vo 登录日志 VO
     * @return 登录日志 DTO
     */
    public static LoginLogDTO toLoginLogDTOFromVO(LoginLogVO vo) {
        if (vo == null) {
            return null;
        }
        return LoginLogDTO.builder()
                .id(vo.getId())
                .userId(vo.getUserId())
                .username(vo.getUsername())
                .ip(vo.getLoginIp())
                .userAgent(vo.getUserAgent())
                .loginResult(vo.getLoginResult())
                .loginTime(vo.getLoginTime())
                .build();
    }

    /**
     * 将 {@link LoginLogVO} 列表转换为 {@link LoginLogDTO} 列表。
     *
     * @param voList 登录日志 VO 列表
     * @return 登录日志 DTO 列表
     */
    public static List<LoginLogDTO> toLoginLogDTOListFromVO(List<LoginLogVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(IdentityApiConverter::toLoginLogDTOFromVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // LoginLog Entity → LoginLogDTO 转换
    // ============================================================

    /**
     * 将 {@link LoginLog} Entity 转换为 {@link LoginLogDTO}。
     *
     * @param entity 登录日志 Entity
     * @return 登录日志 DTO
     */
    public static LoginLogDTO toLoginLogDTO(LoginLog entity) {
        if (entity == null) {
            return null;
        }
        return LoginLogDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .ip(entity.getLoginIp())
                .userAgent(entity.getUserAgent())
                .loginResult(entity.getLoginResult() != null ? entity.getLoginResult().getCode() : null)
                .loginTime(entity.getCreateTime())
                .build();
    }

    // ============================================================
    // LoginVO → LoginResult 转换
    // ============================================================

    /**
     * 将 {@link LoginVO} 转换为 {@link LoginResult}。
     *
     * <p>旧 AuthService.login 返回 LoginVO，需转换为 API 层 LoginResult。
     * tokenType 固定为 "Bearer"，expiresIn 旧 VO 未提供，设为 null。
     *
     * @param vo 登录 VO
     * @return 登录结果
     */
    public static LoginResult toLoginResult(LoginVO vo) {
        if (vo == null) {
            return null;
        }
        return LoginResult.builder()
                .accessToken(vo.getAccessToken())
                .refreshToken(vo.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(null)
                .user(toSnapshotFromVO(vo.getUser()))
                .build();
    }

    // ============================================================
    // TokenVO → TokenResult 转换
    // ============================================================

    /**
     * 将 {@link TokenVO} 转换为 {@link TokenResult}。
     *
     * @param vo 令牌 VO
     * @return 令牌结果
     */
    public static TokenResult toTokenResult(TokenVO vo) {
        if (vo == null) {
            return null;
        }
        return TokenResult.builder()
                .accessToken(vo.getAccessToken())
                .refreshToken(vo.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(null)
                .build();
    }

    // ============================================================
    // CaptchaVO → CaptchaResult 转换
    // ============================================================

    /**
     * 将 {@link CaptchaVO} 转换为 {@link CaptchaResult}。
     *
     * @param vo 验证码 VO
     * @return 验证码结果
     */
    public static CaptchaResult toCaptchaResult(CaptchaVO vo) {
        if (vo == null) {
            return null;
        }
        return CaptchaResult.builder()
                .captchaId(vo.getCaptchaId())
                .captchaImage(vo.getCaptchaImage())
                .build();
    }

    // ============================================================
    // PageResult<UserVO> → PageResult<UserSummaryDTO> 转换
    // ============================================================

    /**
     * 将 {@link PageResult}<{@link UserVO}> 转换为
     * {@link PageResult}<{@link UserSummaryDTO}>。
     *
     * @param voPage VO 分页结果
     * @return DTO 分页结果
     */
    public static PageResult<UserSummaryDTO> toSummaryDTOPage(PageResult<UserVO> voPage) {
        if (voPage == null) {
            return null;
        }
        List<UserSummaryDTO> dtoList = voPage.getList() == null ? Collections.emptyList()
                : voPage.getList().stream()
                        .map(IdentityApiConverter::toSummaryDTOFromVO)
                        .collect(Collectors.toList());
        return PageResult.of(dtoList, voPage.getTotal(), voPage.getPageNum(), voPage.getPageSize());
    }

    /**
     * 将 {@link PageResult}<{@link LoginLogVO}> 转换为
     * {@link PageResult}<{@link LoginLogDTO}>。
     *
     * @param voPage VO 分页结果
     * @return DTO 分页结果
     */
    public static PageResult<LoginLogDTO> toLoginLogDTOPage(PageResult<LoginLogVO> voPage) {
        if (voPage == null) {
            return null;
        }
        List<LoginLogDTO> dtoList = voPage.getList() == null ? Collections.emptyList()
                : voPage.getList().stream()
                        .map(IdentityApiConverter::toLoginLogDTOFromVO)
                        .collect(Collectors.toList());
        return PageResult.of(dtoList, voPage.getTotal(), voPage.getPageNum(), voPage.getPageSize());
    }

    // ============================================================
    // Command → 旧 Request 转换（API 层 Command → 旧 DTO Request）
    // ============================================================

    /**
     * 将 {@link RegisterCommand} 转换为旧 {@link RegisterRequest}。
     *
     * @param cmd 注册命令
     * @return 旧注册请求
     */
    public static RegisterRequest toRegisterRequest(RegisterCommand cmd) {
        if (cmd == null) {
            return null;
        }
        RegisterRequest req = new RegisterRequest();
        req.setUsername(cmd.getUsername());
        req.setPassword(cmd.getPassword());
        req.setPhone(cmd.getPhone());
        req.setCaptchaKey(cmd.getCaptchaId());
        req.setCaptchaCode(cmd.getCaptchaCode());
        return req;
    }

    /**
     * 将 {@link LoginCommand} 转换为旧 {@link LoginRequest}。
     *
     * @param cmd 登录命令
     * @return 旧登录请求
     */
    public static LoginRequest toLoginRequest(LoginCommand cmd) {
        if (cmd == null) {
            return null;
        }
        LoginRequest req = new LoginRequest();
        req.setUsername(cmd.getUsername());
        req.setPassword(cmd.getPassword());
        req.setCaptchaKey(cmd.getCaptchaId());
        req.setCaptchaCode(cmd.getCaptchaCode());
        return req;
    }

    /**
     * 将 {@link ChangePasswordCommand} 转换为旧 {@link ChangePasswordRequest}。
     *
     * <p>注意：旧 Request 包含 confirmPassword 和 code 字段，Command 未提供，
     * 设为 null，由旧 Service 内部处理（或 Controller 层补齐）。
     *
     * @param cmd 修改密码命令
     * @return 旧修改密码请求
     */
    public static ChangePasswordRequest toChangePasswordRequest(ChangePasswordCommand cmd) {
        if (cmd == null) {
            return null;
        }
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword(cmd.getOldPassword());
        req.setNewPassword(cmd.getNewPassword());
        return req;
    }

    /**
     * 将 {@link UpdateProfileCommand} 转换为旧 {@link ProfileUpdateRequest}。
     *
     * @param cmd 更新资料命令
     * @return 旧资料更新请求
     */
    public static ProfileUpdateRequest toProfileUpdateRequest(UpdateProfileCommand cmd) {
        if (cmd == null) {
            return null;
        }
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname(cmd.getNickname());
        req.setEmail(cmd.getEmail());
        req.setPhone(cmd.getPhone());
        req.setAvatar(cmd.getAvatarUrl());
        return req;
    }

    /**
     * 将 {@link RefreshTokenCommand} 转换为旧 {@link RefreshTokenRequest}。
     *
     * @param cmd 刷新令牌命令
     * @return 旧刷新令牌请求
     */
    public static RefreshTokenRequest toRefreshTokenRequest(RefreshTokenCommand cmd) {
        if (cmd == null) {
            return null;
        }
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(cmd.getRefreshToken());
        return req;
    }

    /**
     * 将 {@link SendCodeCommand} 转换为旧 {@link ForgotPasswordSendRequest}。
     *
     * @param cmd 发送验证码命令
     * @return 旧发送验证码请求
     */
    public static ForgotPasswordSendRequest toForgotPasswordSendRequest(SendCodeCommand cmd) {
        if (cmd == null) {
            return null;
        }
        ForgotPasswordSendRequest req = new ForgotPasswordSendRequest();
        req.setType(cmd.getType());
        req.setAccount(cmd.getAccount());
        return req;
    }

    /**
     * 将 {@link ResetPasswordCommand} 转换为旧 {@link ForgotPasswordResetRequest}。
     *
     * @param cmd 重置密码命令
     * @return 旧重置密码请求
     */
    public static ForgotPasswordResetRequest toForgotPasswordResetRequest(ResetPasswordCommand cmd) {
        if (cmd == null) {
            return null;
        }
        ForgotPasswordResetRequest req = new ForgotPasswordResetRequest();
        req.setType(cmd.getType());
        req.setAccount(cmd.getAccount());
        req.setCode(cmd.getCode());
        req.setNewPassword(cmd.getNewPassword());
        return req;
    }

    /**
     * 将 {@link UserListQuery} 转换为旧 {@link UserListRequest}。
     *
     * @param query 用户列表查询条件
     * @return 旧用户列表请求
     */
    public static UserListRequest toUserListRequest(UserListQuery query) {
        if (query == null) {
            return null;
        }
        UserListRequest req = new UserListRequest();
        req.setPageNum(query.getPageNum() != null ? query.getPageNum() : 1);
        req.setPageSize(query.getPageSize() != null ? query.getPageSize() : 10);
        req.setRole(query.getRole());
        req.setStatus(query.getStatus());
        req.setKeyword(query.getKeyword());
        return req;
    }

    // ============================================================
    // SaveAddressCommand → UserAddressVO 转换（Command → 旧 VO）
    // ============================================================

    /**
     * 将 {@link SaveAddressCommand} 转换为旧 {@link UserAddressVO}。
     *
     * @param cmd 保存地址命令
     * @return 旧地址 VO
     */
    public static UserAddressVO toUserAddressVO(SaveAddressCommand cmd) {
        if (cmd == null) {
            return null;
        }
        UserAddressVO vo = new UserAddressVO();
        vo.setUserId(cmd.getUserId());
        vo.setReceiverName(cmd.getReceiverName());
        vo.setReceiverPhone(cmd.getReceiverPhone());
        vo.setProvince(cmd.getProvince());
        vo.setCity(cmd.getCity());
        vo.setDistrict(cmd.getDistrict());
        vo.setDetailAddress(cmd.getDetailAddress());
        vo.setIsDefault(cmd.getIsDefault());
        return vo;
    }

    /**
     * 将 {@link UpdateAddressCommand} 转换为旧 {@link UserAddressVO}。
     *
     * @param cmd 更新地址命令
     * @return 旧地址 VO
     */
    public static UserAddressVO toUserAddressVO(UpdateAddressCommand cmd) {
        if (cmd == null) {
            return null;
        }
        UserAddressVO vo = new UserAddressVO();
        vo.setId(cmd.getAddressId());
        vo.setUserId(cmd.getUserId());
        vo.setReceiverName(cmd.getReceiverName());
        vo.setReceiverPhone(cmd.getReceiverPhone());
        vo.setProvince(cmd.getProvince());
        vo.setCity(cmd.getCity());
        vo.setDistrict(cmd.getDistrict());
        vo.setDetailAddress(cmd.getDetailAddress());
        vo.setIsDefault(cmd.getIsDefault());
        return vo;
    }

    // ============================================================
    // String → Enum 转换（Command 中 String → 旧 Service 中 Enum）
    // ============================================================

    /**
     * 将 String 转换为 {@link UserStatus}。
     *
     * @param code 状态码（ACTIVE/LOCKED/DISABLED）
     * @return 用户状态枚举，null 输入返回 null
     */
    public static UserStatus toUserStatus(String code) {
        if (code == null) {
            return null;
        }
        return UserStatus.fromCode(code);
    }

    /**
     * 将 String 转换为 {@link UserRole}。
     *
     * @param code 角色码（BUYER/SELLER/ADMIN）
     * @return 用户角色枚举，null 输入返回 null
     */
    public static UserRole toUserRole(String code) {
        if (code == null) {
            return null;
        }
        return UserRole.fromCode(code);
    }

    // ============================================================
    // 反向转换：UserSnapshot → UserVO（Snapshot → VO，供 Controller 切换后保持前端返回结构）
    // ============================================================

    /**
     * 将 {@link UserSnapshot} 反向转换为 {@link UserVO}。
     *
     * <p>供 Controller 切换到 {@code UserApi} 后保持前端返回结构不变。
     *
     * @param snapshot 用户快照
     * @return 用户 VO
     */
    public static UserVO toUserVO(UserSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(snapshot.getId());
        vo.setUsername(snapshot.getUsername());
        vo.setPhone(snapshot.getPhone());
        vo.setEmail(snapshot.getEmail());
        vo.setNickname(snapshot.getNickname());
        vo.setAvatar(snapshot.getAvatarUrl());
        vo.setRole(snapshot.getRole());
        vo.setStatus(snapshot.getStatus());
        return vo;
    }

    /**
     * 将 {@link UserSummaryDTO} 反向转换为 {@link UserVO}。
     *
     * @param dto 用户摘要 DTO
     * @return 用户 VO
     */
    public static UserVO toUserVO(UserSummaryDTO dto) {
        if (dto == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(dto.getId());
        vo.setUsername(dto.getUsername());
        vo.setPhone(dto.getPhone());
        vo.setEmail(dto.getEmail());
        vo.setNickname(dto.getNickname());
        vo.setAvatar(dto.getAvatarUrl());
        vo.setRole(dto.getRole());
        vo.setStatus(dto.getStatus());
        vo.setCreateTime(dto.getCreateTime());
        return vo;
    }

    /**
     * 将 {@link PageResult}<{@link UserSummaryDTO}> 反向转换为
     * {@link PageResult}<{@link UserVO}>。
     *
     * @param dtoPage DTO 分页结果
     * @return VO 分页结果
     */
    public static PageResult<UserVO> toUserVOPage(PageResult<UserSummaryDTO> dtoPage) {
        if (dtoPage == null) {
            return null;
        }
        List<UserVO> voList = dtoPage.getList() == null ? Collections.emptyList()
                : dtoPage.getList().stream()
                        .map(IdentityApiConverter::toUserVO)
                        .collect(Collectors.toList());
        return PageResult.of(voList, dtoPage.getTotal(), dtoPage.getPageNum(), dtoPage.getPageSize());
    }

    // ============================================================
    // 反向转换：AddressDTO → UserAddressVO（DTO → VO）
    // ============================================================

    /**
     * 将 {@link AddressDTO} 反向转换为 {@link UserAddressVO}。
     *
     * @param dto 地址 DTO
     * @return 地址 VO
     */
    public static UserAddressVO toUserAddressVO(AddressDTO dto) {
        if (dto == null) {
            return null;
        }
        UserAddressVO vo = new UserAddressVO();
        vo.setId(dto.getId());
        vo.setUserId(dto.getUserId());
        vo.setReceiverName(dto.getReceiverName());
        vo.setReceiverPhone(dto.getReceiverPhone());
        vo.setProvince(dto.getProvince());
        vo.setCity(dto.getCity());
        vo.setDistrict(dto.getDistrict());
        vo.setDetailAddress(dto.getDetailAddress());
        vo.setIsDefault(dto.getIsDefault());
        return vo;
    }

    /**
     * 将 {@link AddressDTO} 列表反向转换为 {@link UserAddressVO} 列表。
     *
     * @param dtoList 地址 DTO 列表
     * @return 地址 VO 列表
     */
    public static List<UserAddressVO> toUserAddressVOList(List<AddressDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(IdentityApiConverter::toUserAddressVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 反向转换：FavoriteItemDTO → FavoriteItemVO（DTO → VO）
    // ============================================================

    /**
     * 将 {@link FavoriteItemDTO} 反向转换为 {@link FavoriteItemVO}。
     *
     * @param dto 收藏项 DTO
     * @return 收藏项 VO
     */
    public static FavoriteItemVO toFavoriteItemVO(FavoriteItemDTO dto) {
        if (dto == null) {
            return null;
        }
        FavoriteItemVO vo = new FavoriteItemVO();
        vo.setId(dto.getId());
        vo.setProductId(dto.getProductId());
        vo.setProductName(dto.getProductName());
        vo.setMainImage(dto.getProductMainImage());
        vo.setOriginalPrice(dto.getProductPrice());
        vo.setProductStatus(dto.getProductStatus());
        return vo;
    }

    /**
     * 将 {@link FavoriteItemDTO} 列表反向转换为 {@link FavoriteItemVO} 列表。
     *
     * @param dtoList 收藏项 DTO 列表
     * @return 收藏项 VO 列表
     */
    public static List<FavoriteItemVO> toFavoriteItemVOList(List<FavoriteItemDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(IdentityApiConverter::toFavoriteItemVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 反向转换：LoginResult → LoginVO（Result → VO）
    // ============================================================

    /**
     * 将 {@link LoginResult} 反向转换为 {@link LoginVO}。
     *
     * @param result 登录结果
     * @return 登录 VO
     */
    public static LoginVO toLoginVO(LoginResult result) {
        if (result == null) {
            return null;
        }
        LoginVO vo = new LoginVO();
        vo.setAccessToken(result.getAccessToken());
        vo.setRefreshToken(result.getRefreshToken());
        vo.setUser(toUserVO(result.getUser()));
        return vo;
    }

    /**
     * 将 {@link TokenResult} 反向转换为 {@link TokenVO}。
     *
     * @param result 令牌结果
     * @return 令牌 VO
     */
    public static TokenVO toTokenVO(TokenResult result) {
        if (result == null) {
            return null;
        }
        TokenVO vo = new TokenVO();
        vo.setAccessToken(result.getAccessToken());
        vo.setRefreshToken(result.getRefreshToken());
        return vo;
    }

    /**
     * 将 {@link CaptchaResult} 反向转换为 {@link CaptchaVO}。
     *
     * @param result 验证码结果
     * @return 验证码 VO
     */
    public static CaptchaVO toCaptchaVO(CaptchaResult result) {
        if (result == null) {
            return null;
        }
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(result.getCaptchaId());
        vo.setCaptchaImage(result.getCaptchaImage());
        return vo;
    }

    // ============================================================
    // 反向转换：LoginLogDTO → LoginLogVO（DTO → VO）
    // ============================================================

    /**
     * 将 {@link LoginLogDTO} 反向转换为 {@link LoginLogVO}。
     *
     * @param dto 登录日志 DTO
     * @return 登录日志 VO
     */
    public static LoginLogVO toLoginLogVO(LoginLogDTO dto) {
        if (dto == null) {
            return null;
        }
        LoginLogVO vo = new LoginLogVO();
        vo.setId(dto.getId());
        vo.setUserId(dto.getUserId());
        vo.setUsername(dto.getUsername());
        vo.setLoginIp(dto.getIp());
        vo.setUserAgent(dto.getUserAgent());
        vo.setLoginResult(dto.getLoginResult());
        vo.setLoginTime(dto.getLoginTime());
        return vo;
    }

    /**
     * 将 {@link PageResult}<{@link LoginLogDTO}> 反向转换为
     * {@link PageResult}<{@link LoginLogVO}>。
     *
     * @param dtoPage DTO 分页结果
     * @return VO 分页结果
     */
    public static PageResult<LoginLogVO> toLoginLogVOPage(PageResult<LoginLogDTO> dtoPage) {
        if (dtoPage == null) {
            return null;
        }
        List<LoginLogVO> voList = dtoPage.getList() == null ? Collections.emptyList()
                : dtoPage.getList().stream()
                        .map(IdentityApiConverter::toLoginLogVO)
                        .collect(Collectors.toList());
        return PageResult.of(voList, dtoPage.getTotal(), dtoPage.getPageNum(), dtoPage.getPageSize());
    }
}