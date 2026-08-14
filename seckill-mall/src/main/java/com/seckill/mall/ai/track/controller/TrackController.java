package com.seckill.mall.ai.track.controller;

import com.seckill.mall.ai.track.dto.TrackEventRequest;
import com.seckill.mall.ai.track.service.TrackService;
import com.seckill.mall.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 埋点上报接口
 * <p>前端聚合一批行为事件后批量上报，服务端投递 MQ 异步落库。
 * <p>路径 {@code /api/v1/track/**} 已在 {@link com.seckill.mall.config.SecurityConfig}
 * 白名单中 permitAll，允许未登录用户上报（userId 为 null）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TrackController.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/track")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /**
     * 批量上报埋点事件。
     * <p>异步处理：仅投递 MQ 立即返回成功，落库由消费者异步完成。
     *
     * @param req 埋点请求
     * @return 成功响应
     */
    @PostMapping("/event")
    public Result<Void> track(@Valid @RequestBody TrackEventRequest req) {
        trackService.batchTrack(req.getEvents());
        return Result.success();
    }
}