package com.seckill.mall.service;

import com.seckill.mall.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UploadService.java
 * 邮箱：nj651217@163.com
 */
public interface UploadService {

    /**
     * 通用图片上传
     *
     * @param file    上传的图片文件（必填）
     * @param bizType 业务类型（可选，product/seckill/avatar/category）
     * @param bizId   业务 ID（可选）
     * @return 上传结果视图对象
     */
    UploadResultVO uploadImage(MultipartFile file, String bizType, Long bizId);
}