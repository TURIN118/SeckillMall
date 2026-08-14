package com.seckill.mall.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 导购助手请求 DTO。
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code message} —— 用户自然语言消息（必填，最长 500 字符）</li>
 *   <li>{@code conversationId} —— 会话 ID（可选），用于多轮对话历史关联；
 *       未登录用户传 null 时无历史，登录用户可前端生成 UUID 维持多轮上下文</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ShoppingAssistantRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ShoppingAssistantRequest {

    /** 用户自然语言消息 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容最长 500 字符")
    private String message;

    /** 会话 ID（可选，用于多轮对话历史关联） */
    private String conversationId;
}