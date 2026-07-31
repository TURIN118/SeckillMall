package com.seckill.mall.dto;

import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserListRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class UserListRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String role;

    private String status;

    private String keyword;
}
