package com.seckill.mall.payment.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.payment.domain.RechargeCardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值卡实体
 * <p>
 * 对应表 {@code t_recharge_card}，预付费充值卡（卡号唯一，卡密加密存储）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCard.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_recharge_card")
public class RechargeCard {

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 卡号（唯一） */
    private String cardNo;

    /** 卡密（BCrypt 加密存储） */
    private String cardPassword;

    /** 面额 */
    private BigDecimal faceValue;

    /** 状态：UNUSED-未使用 / USED-已使用 / DISABLED-已禁用 */
    @TableField("status")
    private RechargeCardStatus status;

    /** 使用者用户ID */
    private Long usedBy;

    /** 使用时间 */
    private LocalDateTime usedTime;

    /** 批次号 */
    private String batchNo;

    /** 逻辑删除：0-正常 / 1-已删除 */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}