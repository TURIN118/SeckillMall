package com.seckill.mall.upload.application.facade;

import com.seckill.mall.upload.api.dto.UploadResultDTO;
import com.seckill.mall.upload.interfaces.vo.UploadResultVO;

/**
 * Upload API 转换辅助类（Strangler Pattern 门面层）。
 *
 * <p>集中存放 upload 模块旧 VO 与新 API 层 DTO 之间的转换方法，
 * 供 {@link com.seckill.mall.upload.application.UploadApplicationService} 与
 * {@link com.seckill.mall.upload.interfaces.web.UploadController} 调用。
 * 所有方法均为无状态静态方法。
 *
 * <p>转换原则：
 * <ul>
 *     <li>VO ↔ DTO：核心字段一一映射（url/originalName/size/width/height），保持前端契约不变</li>
 *     <li>无字段名/类型差异，纯同构映射</li>
 * </ul>
 *
 * <p>参见 UPLOAD-API-CONTRACT.md。
 *
 * @author wnj
 * @since Phase U.0
 */
public class UploadApiConverter {

    // ============================================================
    // UploadResultVO ↔ UploadResultDTO 转换
    // ============================================================

    /** UploadResultVO → UploadResultDTO（全字段映射） */
    public static UploadResultDTO toDTO(UploadResultVO vo) {
        if (vo == null) {
            return null;
        }
        return UploadResultDTO.builder()
                .url(vo.getUrl())
                .originalName(vo.getOriginalName())
                .size(vo.getSize())
                .width(vo.getWidth())
                .height(vo.getHeight())
                .build();
    }

    /** UploadResultDTO → UploadResultVO（Controller 层前端契约适配，全字段映射） */
    public static UploadResultVO toVO(UploadResultDTO dto) {
        if (dto == null) {
            return null;
        }
        UploadResultVO vo = new UploadResultVO();
        vo.setUrl(dto.getUrl());
        vo.setOriginalName(dto.getOriginalName());
        vo.setSize(dto.getSize());
        vo.setWidth(dto.getWidth());
        vo.setHeight(dto.getHeight());
        return vo;
    }
}