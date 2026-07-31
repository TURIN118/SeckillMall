package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.UserListRequest;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.entity.enums.UserStatus;
import com.seckill.mall.vo.LoginLogVO;
import com.seckill.mall.vo.UserVO;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminUserService.java
 * 邮箱：nj651217@163.com
 */
public interface AdminUserService {

    PageResult<UserVO> getUserList(UserListRequest req);

    void updateUserStatus(Long userId, UserStatus status);

    void updateUserRole(Long userId, UserRole role);

    PageResult<LoginLogVO> getUserLoginLogs(Long userId, Integer pageNum, Integer pageSize);
}
