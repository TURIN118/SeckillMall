package com.seckill.mall.banner.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图实体
 * 对应数据库表 t_banner
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：Banner.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_banner")
public class Banner {

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 轮播图标题 */
    private String title;

    /** 图片URL */
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 排序权重（值越小越靠前） */
    private Integer sortOrder;

    /** 状态：1-启用 / 0-禁用 */
    private Integer status;

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