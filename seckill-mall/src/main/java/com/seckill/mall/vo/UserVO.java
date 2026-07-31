package com.seckill.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String phone;

    private String nickname;

    private String avatar;

    private String role;

    private String status;

    private LocalDateTime createTime;
}
