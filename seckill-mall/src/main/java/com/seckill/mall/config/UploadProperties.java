package com.seckill.mall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UploadProperties.java
 * 邮箱：nj651217@163.com
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    /**
     * 上传文件本地存储根目录
     */
    private String baseDir;

    /**
     * 上传文件访问 URL 前缀
     */
    private String baseUrl;

    /**
     * 允许的文件 MIME 类型列表
     */
    private List<String> allowedTypes;

    /**
     * 单个文件最大字节数
     */
    private Long maxSize;
}