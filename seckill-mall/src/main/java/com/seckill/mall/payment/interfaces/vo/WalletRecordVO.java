package com.seckill.mall.payment.interfaces.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包交易记录视图对象
 * <p>
 * 用于展示用户钱包的交易明细（如充值记录）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：WalletRecordVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class WalletRecordVO {

    /** 记录ID */
    private Long id;

    /** 交易类型：RECHARGE-充值 / CONSUME-消费 / REFUND-退款 */
    private String type;

    /** 交易金额（正数为入账，负数为出账） */
    private BigDecimal amount;

    /** 交易后余额 */
    private BigDecimal balanceAfter;

    /** 交易时间 */
    private LocalDateTime createTime;

    /** 备注 */
    private String remark;
}