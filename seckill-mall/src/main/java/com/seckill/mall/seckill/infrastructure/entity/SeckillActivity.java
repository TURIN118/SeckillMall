package com.seckill.mall.seckill.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillActivity.java
 * 邮箱：nj651217@163.com
 * <p>
 * 秒杀活动场次实体。一个场次可包含多个秒杀商品（t_seckill_goods.activity_id），
 * 场次统一管理 startTime/endTime/perLimit，商品继承场次时间窗口。
 */
@Data
@TableName("t_seckill_activity")
public class SeckillActivity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 场次名称 */
    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 0=待开始 1=进行中 2=已结束 */
    private Integer status;

    /** 每人限购数量 */
    private Integer perLimit;

    private String description;

    /** 场次图片(JSON数组字符串) */
    private String images;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}