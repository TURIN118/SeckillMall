package com.seckill.mall.analytics.tracking.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为埋点实体（append-only，无 is_deleted/update_time）
 * <p>对应表 {@code t_user_event}，由前端批量上报（{@code /api/v1/track/event}）
 * 或后端 {@code @Tracking} 注解切面写入，记录 VIEW/CLICK/ADD_CART/FAVORITE/ORDER/SEARCH
 * 等行为，供 P1 个性化推荐与风控消费。
 * <p>{@code createTime} 由 {@link com.seckill.mall.config.MetaObjectHandler} 自动填充。
 * <p>{@code id} 由 MyBatis-Plus {@link IdType#ASSIGN_ID} 雪花算法生成。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserEvent.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_user_event")
public class UserEvent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录用户 ID（未登录为 null） */
    private Long userId;

    /** 事件类型：VIEW/CLICK/ADD_CART/FAVORITE/ORDER/SEARCH */
    private String eventType;

    /** 目标类型：PRODUCT/CATEGORY/SECKILL/ORDER */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 扩展字段（JSON 字符串：搜索词、页面路径、停留时长等） */
    private String ext;

    /** 设备 ID（前端生成，用于未登录用户跨会话追踪） */
    private String deviceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}