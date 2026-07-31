package com.seckill.mall.vo;

import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TokenVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class TokenVO {

    private String accessToken;

    private String refreshToken;
}
