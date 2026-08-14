package com.seckill.mall.ai.customerservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 客服对话实体（软删除）
 * <p>对应表 {@code t_ai_conversation}（T6 已建表，见 {@code 02_ai_tables.sql}）。
 * 一个用户多个对话，每轮客服会话一行，{@link #status} 标识对话生命周期。
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code id}：雪花 ID（{@link IdType#ASSIGN_ID}）</li>
 *   <li>{@code userId}：归属用户</li>
 *   <li>{@code title}：对话标题（可空，可由首条消息截取生成）</li>
 *   <li>{@code status}：对话状态，取值 ACTIVE/CLOSED/ESCALATED
 *       （活跃/已关闭/已转人工）</li>
 *   <li>{@code createTime}/{@code updateTime}：由
 *       {@link com.seckill.mall.config.MetaObjectHandler} 自动填充</li>
 *   <li>{@code isDeleted}：MyBatis-Plus 逻辑删除（{@link TableLogic}）</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiConversation.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_ai_conversation")
public class AiConversation {

    /** 对话状态：活跃中 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 对话状态：已关闭 */
    public static final String STATUS_CLOSED = "CLOSED";
    /** 对话状态：已转人工 */
    public static final String STATUS_ESCALATED = "ESCALATED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 归属用户 ID */
    private Long userId;

    /** 对话标题（可空） */
    private String title;

    /** 对话状态：ACTIVE/CLOSED/ESCALATED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除，1-已删除 */
    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}