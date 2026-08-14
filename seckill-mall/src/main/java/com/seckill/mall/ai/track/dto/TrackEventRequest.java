package com.seckill.mall.ai.track.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 埋点批量上报请求 DTO
 * <p>前端聚合一批行为事件后通过 {@code POST /api/v1/track/event} 上报，
 * 服务端从 SecurityContext 获取当前 userId（未登录为 null），统一填充后投递 MQ 异步落库。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TrackEventRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class TrackEventRequest {

    /** 事件列表（至少一条） */
    @NotEmpty(message = "事件列表不能为空")
    @Valid
    private List<EventItem> events;

    /**
     * 单条事件项
     */
    @Data
    public static class EventItem {

        /** 事件类型：VIEW/CLICK/ADD_CART/FAVORITE/ORDER/SEARCH */
        @NotBlank(message = "事件类型不能为空")
        private String eventType;

        /** 目标类型：PRODUCT/CATEGORY/SECKILL/ORDER */
        private String targetType;

        /** 目标 ID */
        private Long targetId;

        /** 扩展字段（JSON 字符串：搜索词、页面路径、停留时长等） */
        private String ext;

        /** 设备 ID（前端生成，用于未登录用户跨会话追踪） */
        private String deviceId;
    }
}