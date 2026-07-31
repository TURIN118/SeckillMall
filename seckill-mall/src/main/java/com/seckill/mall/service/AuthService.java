package com.seckill.mall.service;

import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.RefreshTokenRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UserVO;

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
}
