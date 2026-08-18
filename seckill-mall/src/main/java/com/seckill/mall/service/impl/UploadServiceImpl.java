package com.seckill.mall.service.impl;

import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.config.UploadProperties;
import com.seckill.mall.service.StorageService;
import com.seckill.mall.service.UploadService;
import com.seckill.mall.upload.interfaces.vo.UploadResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UploadServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final UploadProperties uploadProperties;
    private final StorageService storageService;

    @Override
    public UploadResultVO uploadImage(MultipartFile file, String bizType, Long bizId) {
        // 1. 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        // 2. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !uploadProperties.getAllowedTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        // 3. 校验文件大小
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 4. 确定业务子目录
        String type = (bizType == null || bizType.isBlank()) ? "common" : bizType;

        // 5. 日期路径
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);

        // 6. 生成文件名：uuid + 扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = resolveExtension(originalFilename, contentType);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        // 7. 读取图片宽高（容错处理，先于存储读取避免流被消耗）
        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (IOException e) {
            log.warn("读取图片宽高失败，忽略: {}", originalFilename, e);
        }

        // 8. 委托 StorageService 存储文件，返回可访问 URL
        String directory = type + "/" + datePath;
        String url = storageService.store(file, directory, fileName);

        // 9. 构建返回结果
        UploadResultVO vo = new UploadResultVO();
        vo.setUrl(url);
        vo.setOriginalName(originalFilename);
        vo.setSize(file.getSize());
        vo.setWidth(width);
        vo.setHeight(height);
        return vo;
    }

    /**
     * 解析文件扩展名：优先从原始文件名取，取不到则根据 contentType 兜底。
     */
    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
            if (!ext.isBlank()) {
                return ext.toLowerCase();
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
