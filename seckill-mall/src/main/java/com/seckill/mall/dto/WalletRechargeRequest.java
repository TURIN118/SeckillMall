package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 钱包充值请求（通过充值卡）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：WalletRechargeRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class WalletRechargeRequest {

    /** 卡号 */
    @NotBlank(message = "卡号不能为空")
    @Size(max = 64, message = "卡号最长 64 字符")
    private String cardNo;

    /** 卡密（明文） */
    @NotBlank(message = "卡密不能为空")
    @Size(max = 128, message = "卡密最长 128 字符")
    private String cardPassword;
}