package com.seckill.mall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务抽象接口
 * 支持 LocalStorage（本地文件系统）和 MinioStorage（MinIO对象存储）等实现
 * 通过配置 storage.type 切换具体实现：
 *   - local: 本地文件系统存储（默认）
 *   - minio: MinIO 对象存储
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：StorageService.java
 * 邮箱：nj651217@163.com
 */
public interface StorageService {

    /**
     * 存储文件
     *
     * @param file      上传的文件
     * @param directory 存储子目录（如 products/2026/08/03）
     * @param filename  存储文件名（如 abc123.jpg）
     * @return 可访问的URL路径（如 /images/products/2026/08/03/abc123.jpg）
     */
    String store(MultipartFile file, String directory, String filename);

    /**
     * 获取文件的完整访问URL
     *
     * @param relativePath 相对路径
     * @return 完整URL
     */
    String getUrl(String relativePath);

    /**
     * 删除文件
     *
     * @param relativePath 相对路径
     */
    void delete(String relativePath);
}