package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值卡生成视图对象
 * <p>
 * 专用于"生成充值卡"接口的返回对象，显式暴露 cardPassword 一次。
 * 普通查询/列表接口应使用 {@link RechargeCardVO}，其 cardPassword 被 @JsonIgnore 屏蔽。
 * <p>
 * C4 修复：不继承 {@link RechargeCardVO}，独立声明所有字段，
 * 避免 Jackson 因父类字段上的 @JsonIgnore 而屏蔽子类的 cardPassword。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardGenerateVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class RechargeCardGenerateVO {

    /** 主键ID */
    private Long id;

    /** 卡号 */
    private String cardNo;

    /**
     * 卡密明文（仅在生成接口返回一次）。
     * <p>
     * 不带 @JsonIgnore，Jackson 默认序列化该字段。
     */
    private String cardPassword;

    /** 面额 */
    private BigDecimal faceValue;

    /** 状态：UNUSED-未使用 / USED-已使用 / DISABLED-已禁用 */
    private String status;

    /** 批次号 */
    private String batchNo;

    /** 创建时间 */
    private LocalDateTime createTime;
}
