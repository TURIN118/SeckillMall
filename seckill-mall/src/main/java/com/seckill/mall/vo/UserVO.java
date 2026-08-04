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

    /**
     * 手机号
     * <p>
     * M31 安全说明：应在 Service 层脱敏(如 138****8000)后再返回前端，
     * 避免完整手机号通过列表/详情接口泄露。
     */
    private String phone;

    /**
     * 邮箱
     * <p>
     * M31 安全说明：应在 Service 层脱敏(如 w***@ex.com)后再返回前端，
     * 避免完整邮箱通过列表/详情接口泄露。
     */
    private String email;

    private String nickname;

    private String avatar;

    private String role;

    private String status;

    private LocalDateTime createTime;
}
