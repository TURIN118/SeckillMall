package com.seckill.mall.controller;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.service.UploadService;
import com.seckill.mall.vo.UploadResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

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

    /** 单文件大小上限：5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    /** bizType 合法取值：字母/数字/下划线/短横线，长度 1-32 */
    private static final Pattern BIZ_TYPE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    private final UploadService uploadService;

    @Operation(summary = "通用图片上传")
    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    public Result<UploadResultVO> uploadImage(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String bizType,
                                              @RequestParam(required = false) Long bizId) {
        // 安全修复（H6）：服务端强校验文件大小，避免大文件 DoS
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        // bizType 白名单校验，防止目录穿越/特殊字符注入
        if (bizType != null && !BIZ_TYPE_PATTERN.matcher(bizType).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "bizType 格式非法");
        }
        return Result.success("上传成功", uploadService.uploadImage(file, bizType, bizId));
    }
}