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
     * <p>
     * 安全修复（M8）：目录穿越风险防护依赖 UploadService 在落盘时使用 UUID 重命名文件，
     * 拒绝保留原始文件名或拼接用户输入的路径。请确保 UploadService 实现满足：
     * 1. 文件名 = UUID + 校验后的扩展名，禁止使用原始文件名；
     * 2. bizType/bizId 参数不可作为路径片段直接拼接到 baseDir；
     * 3. ResourceHandler 配置的 baseDir 必须为只读挂载的独立目录，与应用代码隔离。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(uploadProperties.getBaseUrl() + "/**")
                .addResourceLocations("file:" + uploadProperties.getBaseDir() + "/");
    }
}