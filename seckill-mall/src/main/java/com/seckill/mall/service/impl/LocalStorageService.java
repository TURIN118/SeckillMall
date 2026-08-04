package com.seckill.mall.service.impl;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.config.UploadProperties;
import com.seckill.mall.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 本地文件系统存储实现
 * 默认启用，当 storage.type 未配置或配置为 local 时生效
 *
 * 存储路径：{baseDir}/{directory}/{filename}
 * 访问 URL：{baseUrl}/{directory}/{filename}
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：LocalStorageService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final UploadProperties uploadProperties;

    @Override
    public String store(MultipartFile file, String directory, String filename) {
        File destDir = new File(uploadProperties.getBaseDir(), directory);
        if (!destDir.exists() && !destDir.mkdirs()) {
            log.error("创建存储目录失败: {}", destDir.getAbsolutePath());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "存储目录创建失败");
        }
        File destFile = new File(destDir, filename);
        try {
            file.transferTo(destFile.getAbsoluteFile());
        } catch (IOException e) {
            log.error("写入存储文件失败: {}", destFile.getAbsolutePath(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "存储文件写入失败");
        }
        // 返回URL路径：{baseUrl}/{directory}/{filename}
        return uploadProperties.getBaseUrl() + "/" + directory + "/" + filename;
    }

    @Override
    public String getUrl(String relativePath) {
        return relativePath;
    }

    @Override
    public void delete(String relativePath) {
        // relativePath 格式：/images/products/2026/08/03/abc.jpg
        // 实际文件路径：baseDir + relativePath - baseUrl
        if (relativePath == null) {
            return;
        }
        String basePath = uploadProperties.getBaseDir();
        String baseUrl = uploadProperties.getBaseUrl();
        if (!relativePath.startsWith(baseUrl)) {
            log.warn("非法文件路径，relativePath={}, baseUrl={}", relativePath, baseUrl);
            return;
        }
        String filePath = basePath + relativePath.substring(baseUrl.length());
        File file = new File(filePath);
        if (file.exists() && !file.delete()) {
            log.warn("删除文件失败: {}", filePath);
        }
    }
}