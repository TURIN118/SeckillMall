package com.seckill.mall.ai.gateway.service;

import com.seckill.mall.ai.gateway.config.ModelRouteProperties;
import com.seckill.mall.ai.gateway.dto.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 多模型路由策略服务。
 * <p>按场景解析目标模型名，P0 阶段只接 DeepSeek，通义/文心预留路由配置但不真实调用。
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private static final String DEFAULT_MODEL = "deepseek-chat";

    private final ModelRouteProperties properties;

    public RouteService(ModelRouteProperties properties) {
        this.properties = properties;
    }

    /**
     * 按场景解析目标模型名。
     *
     * @param scene 调用场景
     * @return 模型名（如 deepseek-chat/qwen-plus/ernie-bot），未配置时兜底 deepseek-chat
     */
    public String resolveModel(Scene scene) {
        if (properties == null || properties.getRoute() == null) {
            log.warn("路由配置为空，兜底使用 {}", DEFAULT_MODEL);
            return DEFAULT_MODEL;
        }
        String model = properties.getRoute().get(scene.name().toLowerCase());
        if (model == null) {
            log.warn("场景 {} 未配置路由，兜底使用 {}", scene, DEFAULT_MODEL);
            return DEFAULT_MODEL;
        }
        log.debug("场景 {} 路由到模型 {}", scene, model);
        return model;
    }
}