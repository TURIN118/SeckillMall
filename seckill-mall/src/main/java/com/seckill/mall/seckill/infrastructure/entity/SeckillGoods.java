package com.seckill.mall.seckill.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.seckill.domain.SeckillStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillGoods.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_seckill_goods")
public class SeckillGoods {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    /** 关联秒杀场次 ID（场次化重构后新增，可为空表示独立活动） */
    private Long activityId;

    private BigDecimal seckillPrice;

    private Integer stockCount;

    private Integer availableCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField("status")
    private SeckillStatus status;

    private Long creatorId;

    /** 秒杀活动名称 */
    private String seckillName;

    /** 每人限购数量 */
    private Integer perLimit;

    /** 活动图片(JSON数组字符串) */
    private String images;

    /** 活动描述 */
    private String description;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
