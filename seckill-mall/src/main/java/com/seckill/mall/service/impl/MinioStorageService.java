package com.seckill.mall.service.impl;

import com.seckill.mall.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 对象存储实现（预留骨架）
 * 当 storage.type = minio 时启用
 *
 * 启用前需在 application.yml 中配置 minio 连接信息：
 * <pre>
 * minio:
 *   endpoint: http://localhost:9000
 *   access-key: minioadmin
 *   secret-key: minioadmin
 *   bucket: seckill-mall
 * </pre>
 *
 * 当前为预留实现，所有方法抛出 UnsupportedOperationException。
 * 后续接入 MinIO 时，注入 MinioClient 并实现以下方法：
 *   - store: 调用 MinioClient.putObject() 上传文件
 *   - getUrl: 拼接 endpoint + bucket + objectName
 *   - delete: 调用 MinioClient.removeObject() 删除文件
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：MinioStorageService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    /**
     * MinIO 客户端，启用后通过构造器注入：
     *   private final MinioClient minioClient;
     *   private final String bucket;
     */

    @Override
    public String store(MultipartFile file, String directory, String filename) {
        // TODO: 接入 MinIO SDK 后实现
        // String objectName = directory + "/" + filename;
        // minioClient.putObject(PutObjectArgs.builder()
        //         .bucket(bucket).object(objectName)
        //         .stream(file.getInputStream(), file.getSize(), -1)
        //         .contentType(file.getContentType())
        //         .build());
        // return getUrl(objectName);
        throw new UnsupportedOperationException("MinIO存储尚未配置，请在application.yml中配置minio连接信息");
    }

    @Override
    public String getUrl(String relativePath) {
        // TODO: 接入 MinIO SDK 后实现
        // return endpoint + "/" + bucket + "/" + relativePath;
        throw new UnsupportedOperationException("MinIO存储尚未配置，请在application.yml中配置minio连接信息");
    }

    @Override
    public void delete(String relativePath) {
        // TODO: 接入 MinIO SDK 后实现
        // minioClient.removeObject(RemoveObjectArgs.builder()
        //         .bucket(bucket).object(relativePath)
        //         .build());
        throw new UnsupportedOperationException("MinIO存储尚未配置，请在application.yml中配置minio连接信息");
    }
}