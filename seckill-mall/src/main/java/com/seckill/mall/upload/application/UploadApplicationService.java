package com.seckill.mall.upload.application;

import com.seckill.mall.service.UploadService;
import com.seckill.mall.upload.api.UploadApi;
import com.seckill.mall.upload.api.dto.UploadResultDTO;
import com.seckill.mall.upload.application.facade.UploadApiConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload 模块 Application 层门面服务（Strangler Pattern）。
 *
 * <p>实现 {@link UploadApi}，作为新 API 层与旧 {@link UploadService} 实现之间的
 * 绞杀者门面（Strangler Facade）。本类不重写任何业务逻辑，仅做：
 * <ul>
 *     <li>委托：所有方法体调用旧 UploadService 对应方法</li>
 *     <li>适配：通过 {@link UploadApiConverter} 将旧 VO 返回值转换为新 DTO</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *     <li>不修改旧 UploadService / UploadServiceImpl 的任何业务行为</li>
 *     <li>不引入新的 SQL / Mapper / 跨模块 Service 依赖</li>
 *     <li>保持旧 UploadService 在 Spring 容器中仍可被其他潜在调用方注入（向后兼容）</li>
 * </ul>
 *
 * <p>迁移路径：Phase U.2 完成后，UploadController 切换为依赖 UploadApi（本类），
 * 旧 UploadService 仅被本类与 AuthServiceImpl 引用。后续 Phase 可将 UploadServiceImpl 的业务逻辑
 * 平滑迁入本类或新建的领域服务，再删除旧 UploadService。
 *
 * <p>参见 UPLOAD-API-CONTRACT.md。
 *
 * @author wnj
 * @since Phase U.0
 */
@Service
@RequiredArgsConstructor
public class UploadApplicationService implements UploadApi {

    private final UploadService uploadService;

    @Override
    public UploadResultDTO uploadImage(MultipartFile file, String bizType, Long bizId) {
        return UploadApiConverter.toDTO(uploadService.uploadImage(file, bizType, bizId));
    }
}