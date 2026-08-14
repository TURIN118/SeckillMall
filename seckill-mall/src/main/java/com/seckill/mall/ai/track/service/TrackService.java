package com.seckill.mall.ai.track.service;

import com.seckill.mall.ai.track.dto.TrackEventRequest;
import com.seckill.mall.ai.track.entity.UserEvent;
import com.seckill.mall.ai.track.mq.TrackProducer;
import com.seckill.mall.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 埋点服务
 * <p>两类入口：
 * <ol>
 *   <li>{@link #batchTrack(List)}：前端批量上报接口调用，从 SecurityContext 获取当前
 *       userId（未登录为 null），构建 {@link UserEvent} 列表后通过 {@link TrackProducer}
 *       投递 MQ 异步落库。</li>
 *   <li>{@link #track(UserEvent)}：{@code @Tracking} 注解切面调用，单条埋点投递 MQ。</li>
 * </ol>
 * <p>本服务只负责"组装 + 投递 MQ"，不直接落库，落库由 {@link com.seckill.mall.ai.track.mq.TrackConsumer} 完成。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TrackService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackProducer trackProducer;
    private final SecurityUtils securityUtils;

    /**
     * 批量埋点上报（Controller 调用）。
     * <p>从 SecurityContext 获取当前 userId（未登录为 null），逐条构建 UserEvent，
     * 统一投递 MQ。userId 获取失败（未登录）不阻断流程，置 null 即可。
     *
     * @param events 事件项列表
     */
    public void batchTrack(List<TrackEventRequest.EventItem> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        Long userId = currentUserIdOrNull();
        List<UserEvent> list = new ArrayList<>(events.size());
        for (TrackEventRequest.EventItem item : events) {
            UserEvent event = new UserEvent();
            event.setUserId(userId);
            event.setEventType(item.getEventType());
            event.setTargetType(item.getTargetType());
            event.setTargetId(item.getTargetId());
            event.setExt(item.getExt());
            event.setDeviceId(item.getDeviceId());
            list.add(event);
        }
        trackProducer.send(list);
    }

    /**
     * 单条埋点（@Tracking 切面调用）。
     * <p>切面已组装好 UserEvent（含 userId、targetId 等），此处直接投递 MQ。
     *
     * @param event 已组装完成的埋点事件
     */
    public void track(UserEvent event) {
        if (event == null) {
            return;
        }
        // 切面可能未设置 userId（如未登录场景），此处兜底补充
        if (event.getUserId() == null) {
            event.setUserId(currentUserIdOrNull());
        }
        trackProducer.send(List.of(event));
    }

    /**
     * 安全获取当前 userId，未登录或异常返回 null（埋点允许匿名）。
     */
    private Long currentUserIdOrNull() {
        try {
            return securityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}