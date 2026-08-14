package com.seckill.mall.ai.customerservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 客服消息实体（append-only，无 is_deleted）
 * <p>对应表 {@code t_ai_message}（T6 已建表，见 {@code 02_ai_tables.sql}）。
 * 一个对话多条消息，{@link #role} 标识消息来源（user/assistant/system），
 * 仅追加不修改不删除，保证对话历史完整可审计。
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code id}：雪花 ID（{@link IdType#ASSIGN_ID}）</li>
 *   <li>{@code conversationId}：所属对话 ID（外键逻辑关联，无物理外键）</li>
 *   <li>{@code role}：消息角色，取值 user/assistant/system</li>
 *   <li>{@code content}：消息文本内容</li>
 *   <li>{@code tokens}：本条消息 token 数（用于成本统计，可空为 0）</li>
 *   <li>{@code createTime}：由 {@link com.seckill.mall.config.MetaObjectHandler} 自动填充</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiMessage.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_ai_message")
public class AiMessage {

    /** 消息角色：用户消息 */
    public static final String ROLE_USER = "user";
    /** 消息角色：助手消息 */
    public static final String ROLE_ASSISTANT = "assistant";
    /** 消息角色：系统消息 */
    public static final String ROLE_SYSTEM = "system";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属对话 ID */
    private Long conversationId;

    /** 消息角色：user/assistant/system */
    private String role;

    /** 消息文本内容 */
    private String content;

    /** 本条消息 token 数（默认 0） */
    private Integer tokens;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}