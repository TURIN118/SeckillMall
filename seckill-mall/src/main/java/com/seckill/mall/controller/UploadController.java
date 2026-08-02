package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.service.UploadService;
import com.seckill.mall.vo.UploadResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UploadController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "文件上传", description = "通用图片上传")
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Operation(summary = "通用图片上传")
    @PostMapping("/image")
    public Result<UploadResultVO> uploadImage(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String bizType,
                                              @RequestParam(required = false) Long bizId) {
        return Result.success("上传成功", uploadService.uploadImage(file, bizType, bizId));
    }
}