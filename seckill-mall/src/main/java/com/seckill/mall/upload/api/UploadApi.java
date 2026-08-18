package com.seckill.mall.upload.api;

import com.seckill.mall.upload.api.dto.UploadResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload 模块通用图片上传 API。
 *
 * <p>对外暴露通用图片上传能力，供 UploadController 与其他模块（如 AuthService 头像上传）调用。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * @author wnj
 * @since Phase U.0
 */
public interface UploadApi {

    /**
     * 通用图片上传。
     *
     * @param file    上传的图片文件（必填）
     * @param bizType 业务类型（可选，product/seckill/avatar/category）
     * @param bizId   业务 ID（可选）
     * @return 上传结果 DTO
     */
    UploadResultDTO uploadImage(MultipartFile file, String bizType, Long bizId);
}