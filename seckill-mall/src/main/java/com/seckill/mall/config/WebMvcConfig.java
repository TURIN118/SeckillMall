package com.seckill.mall.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：WebMvcConfig.java
 * 邮箱：nj651217@163.com
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    /**
     * 将上传访问 URL 前缀映射到本地磁盘存储目录，使上传后的图片可通过静态资源方式访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(uploadProperties.getBaseUrl() + "/**")
                .addResourceLocations("file:" + uploadProperties.getBaseDir() + "/");
    }
}