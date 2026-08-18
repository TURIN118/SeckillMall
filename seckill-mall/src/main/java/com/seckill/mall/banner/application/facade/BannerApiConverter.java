package com.seckill.mall.banner.application.facade;

import com.seckill.mall.banner.api.dto.BannerDTO;
import com.seckill.mall.banner.interfaces.vo.BannerVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Banner API 转换辅助类（Strangler Pattern 门面层）。
 *
 * <p>集中存放 banner 模块旧 VO 与新 API 层 DTO 之间的转换方法，
 * 供 {@link com.seckill.mall.banner.application.BannerApplicationService} 与
 * {@link com.seckill.mall.banner.interfaces.web.BannerController} /
 * {@link com.seckill.mall.banner.interfaces.web.BannerPublicController} 调用。
 * 所有方法均为无状态静态方法。
 *
 * <p>转换原则：
 * <ul>
 *     <li>VO ↔ DTO：核心字段一一映射（id/title/imageUrl/linkUrl/sortOrder/status/createTime/updateTime），保持前端契约不变</li>
 *     <li>无字段名/类型差异，纯同构映射</li>
 *     <li>列表转换：保持顺序，null 列表返回 null</li>
 * </ul>
 *
 * <p>参见 BANNER-API-CONTRACT.md。
 *
 * @author wnj
 * @since Phase B.0
 */
public class BannerApiConverter {

    // ============================================================
    // BannerVO ↔ BannerDTO 转换
    // ============================================================

    /** BannerVO → BannerDTO（全字段映射） */
    public static BannerDTO toDTO(BannerVO vo) {
        if (vo == null) {
            return null;
        }
        return BannerDTO.builder()
                .id(vo.getId())
                .title(vo.getTitle())
                .imageUrl(vo.getImageUrl())
                .linkUrl(vo.getLinkUrl())
                .sortOrder(vo.getSortOrder())
                .status(vo.getStatus())
                .createTime(vo.getCreateTime())
                .updateTime(vo.getUpdateTime())
                .build();
    }

    /** BannerDTO → BannerVO（Controller 层前端契约适配，全字段映射） */
    public static BannerVO toVO(BannerDTO dto) {
        if (dto == null) {
            return null;
        }
        BannerVO vo = new BannerVO();
        vo.setId(dto.getId());
        vo.setTitle(dto.getTitle());
        vo.setImageUrl(dto.getImageUrl());
        vo.setLinkUrl(dto.getLinkUrl());
        vo.setSortOrder(dto.getSortOrder());
        vo.setStatus(dto.getStatus());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    /** List&lt;BannerVO&gt; → List&lt;BannerDTO&gt;（保持顺序，null 列表返回 null） */
    public static List<BannerDTO> toDTOList(List<BannerVO> vos) {
        if (vos == null) {
            return null;
        }
        return vos.stream().map(BannerApiConverter::toDTO).collect(Collectors.toList());
    }

    /** List&lt;BannerDTO&gt; → List&lt;BannerVO&gt;（保持顺序，null 列表返回 null） */
    public static List<BannerVO> toVOList(List<BannerDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream().map(BannerApiConverter::toVO).collect(Collectors.toList());
    }
}