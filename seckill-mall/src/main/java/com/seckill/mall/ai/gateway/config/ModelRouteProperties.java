package com.seckill.mall.ai.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

/**
 * 多模型路由配置属性。
 * <p>绑定 application-ai.yml 中 ai.models.route 配置。
 */
@ConfigurationProperties(prefix = "ai.models")
public class ModelRouteProperties {

    /** 路由规则：{reasoning: deepseek-chat, chinese: qwen-plus, compliance: ernie-bot} */
    private Map<String, String> route;

    public Map<String, String> getRoute() {
        return route;
    }

    public void setRoute(Map<String, String> route) {
        this.route = route;
    }
}