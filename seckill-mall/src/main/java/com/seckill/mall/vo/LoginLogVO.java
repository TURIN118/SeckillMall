package com.seckill.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：LoginLogVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class LoginLogVO {

    private Long id;

    private Long userId;

    private String username;

    private String loginIp;

    private String loginLocation;

    private String userAgent;

    private String loginResult;

    private String failReason;

    private LocalDateTime loginTime;
}
