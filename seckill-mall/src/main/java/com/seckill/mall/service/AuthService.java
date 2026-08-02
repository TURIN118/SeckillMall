package com.seckill.mall.service;

import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.ProfileUpdateRequest;
import com.seckill.mall.dto.RefreshTokenRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AuthService.java
 * 邮箱：nj651217@163.com
 */
public interface AuthService {

    UserVO register(RegisterRequest req);

    LoginVO login(LoginRequest req, String ip);

    void logout(String accessToken);

    TokenVO refresh(RefreshTokenRequest req);

    UserVO getMe();

    void changePassword(ChangePasswordRequest req);

    /**
     * 更新当前登录用户的个人信息（昵称/邮箱/手机号/头像 URL）
     *
     * @param req 个人信息更新请求（字段均可选）
     * @return 更新后的用户视图对象
     */
    UserVO updateProfile(ProfileUpdateRequest req);

    /**
     * 上传当前登录用户的头像，并持久化头像 URL
     *
     * @param file 头像文件（image/jpeg、image/png、image/webp，最大 2MB）
     * @return 包含 avatar URL 的映射
     */
    Map<String, String> uploadAvatar(MultipartFile file);
}
