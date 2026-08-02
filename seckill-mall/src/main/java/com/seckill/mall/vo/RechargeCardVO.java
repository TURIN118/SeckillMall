package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值卡视图对象
 * <p>
 * 用于后台管理展示，不含卡密（敏感信息）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class RechargeCardVO {

    /** 主键ID */
    private Long id;

    /** 卡号 */
    private String cardNo;

    /** 卡密明文（仅在生成时返回一次，后续查询不返回） */
    private String cardPassword;

    /** 面额 */
    private BigDecimal faceValue;

    /** 状态：UNUSED-未使用 / USED-已使用 / DISABLED-已禁用 */
    private String status;

    /** 使用者用户ID */
    private Long usedBy;

    /** 使用时间 */
    private LocalDateTime usedTime;

    /** 批次号 */
    private String batchNo;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}