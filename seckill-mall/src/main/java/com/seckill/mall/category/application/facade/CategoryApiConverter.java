package com.seckill.mall.category.application.facade;

import com.seckill.mall.category.api.dto.CategoryAttributeDTO;
import com.seckill.mall.category.api.dto.CategoryDTO;
import com.seckill.mall.category.infrastructure.entity.Category;
import com.seckill.mall.category.interfaces.vo.CategoryAttributeVO;
import com.seckill.mall.category.interfaces.vo.CategoryVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Category API 转换器 - VO/Entity 与 DTO 之间的双向转换。
 *
 * <p>供 ApplicationService 与 interfaces/web 层使用：
 * <ul>
 *   <li>ApplicationService 出参：VO/Entity → DTO（toDTO）</li>
 *   <li>Controller 出参：DTO → VO（toVO）</li>
 * </ul>
 *
 * <p>纯静态方法，无状态，不注入 Spring Bean。
 *
 * @author wnj
 * @since Phase CA.4
 */
public final class CategoryApiConverter {

    private CategoryApiConverter() {
    }

    // ==================== Category ====================

    /**
     * CategoryVO → CategoryDTO（递归转换 children）。
     */
    public static CategoryDTO toDTO(CategoryVO vo) {
        if (vo == null) {
            return null;
        }
        CategoryDTO dto = CategoryDTO.builder()
                .id(vo.getId())
                .parentId(vo.getParentId())
                .categoryName(vo.getCategoryName())
                .sortOrder(vo.getSortOrder())
                .status(vo.getStatus())
                .productCount(vo.getProductCount())
                .build();
        if (vo.getChildren() != null) {
            List<CategoryDTO> children = new ArrayList<>(vo.getChildren().size());
            for (CategoryVO child : vo.getChildren()) {
                children.add(toDTO(child));
            }
            dto.setChildren(children);
        }
        return dto;
    }

    /**
     * CategoryDTO → CategoryVO（递归转换 children）。
     */
    public static CategoryVO toVO(CategoryDTO dto) {
        if (dto == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        vo.setId(dto.getId());
        vo.setParentId(dto.getParentId());
        vo.setCategoryName(dto.getCategoryName());
        vo.setSortOrder(dto.getSortOrder());
        vo.setStatus(dto.getStatus());
        vo.setProductCount(dto.getProductCount());
        if (dto.getChildren() != null) {
            List<CategoryVO> children = new ArrayList<>(dto.getChildren().size());
            for (CategoryDTO child : dto.getChildren()) {
                children.add(toVO(child));
            }
            vo.setChildren(children);
        }
        return vo;
    }

    /**
     * Category Entity → CategoryDTO（无 children，productCount 不回填）。
     * 供 getCategoryById 内部调用使用。
     */
    public static CategoryDTO toDTO(Category entity) {
        if (entity == null) {
            return null;
        }
        return CategoryDTO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .categoryName(entity.getName())
                .sortOrder(entity.getSortOrder())
                .status(entity.getStatus())
                .build();
    }

    public static List<CategoryDTO> toDTOList(List<CategoryVO> voList) {
        if (voList == null) {
            return null;
        }
        List<CategoryDTO> result = new ArrayList<>(voList.size());
        for (CategoryVO vo : voList) {
            result.add(toDTO(vo));
        }
        return result;
    }

    public static List<CategoryVO> toVOList(List<CategoryDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<CategoryVO> result = new ArrayList<>(dtoList.size());
        for (CategoryDTO dto : dtoList) {
            result.add(toVO(dto));
        }
        return result;
    }

    // ==================== CategoryAttribute ====================

    /**
     * CategoryAttributeVO → CategoryAttributeDTO（含预设值列表）。
     */
    public static CategoryAttributeDTO toDTO(CategoryAttributeVO vo) {
        if (vo == null) {
            return null;
        }
        CategoryAttributeDTO dto = CategoryAttributeDTO.builder()
                .id(vo.getId())
                .categoryId(vo.getCategoryId())
                .name(vo.getName())
                .type(vo.getType())
                .inputType(vo.getInputType())
                .isRequired(vo.getIsRequired())
                .sortOrder(vo.getSortOrder())
                .build();
        if (vo.getValues() != null) {
            List<CategoryAttributeDTO.CategoryAttributeValueDTO> values =
                    new ArrayList<>(vo.getValues().size());
            for (CategoryAttributeVO.CategoryAttributeValueVO vvo : vo.getValues()) {
                values.add(toDTO(vvo));
            }
            dto.setValues(values);
        }
        return dto;
    }

    /**
     * CategoryAttributeDTO → CategoryAttributeVO（含预设值列表）。
     */
    public static CategoryAttributeVO toVO(CategoryAttributeDTO dto) {
        if (dto == null) {
            return null;
        }
        CategoryAttributeVO vo = new CategoryAttributeVO();
        vo.setId(dto.getId());
        vo.setCategoryId(dto.getCategoryId());
        vo.setName(dto.getName());
        vo.setType(dto.getType());
        vo.setInputType(dto.getInputType());
        vo.setIsRequired(dto.getIsRequired());
        vo.setSortOrder(dto.getSortOrder());
        if (dto.getValues() != null) {
            List<CategoryAttributeVO.CategoryAttributeValueVO> values =
                    new ArrayList<>(dto.getValues().size());
            for (CategoryAttributeDTO.CategoryAttributeValueDTO vd : dto.getValues()) {
                values.add(toVO(vd));
            }
            vo.setValues(values);
        }
        return vo;
    }

    public static List<CategoryAttributeDTO> toAttrDTOList(List<CategoryAttributeVO> voList) {
        if (voList == null) {
            return null;
        }
        List<CategoryAttributeDTO> result = new ArrayList<>(voList.size());
        for (CategoryAttributeVO vo : voList) {
            result.add(toDTO(vo));
        }
        return result;
    }

    public static List<CategoryAttributeVO> toAttrVOList(List<CategoryAttributeDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<CategoryAttributeVO> result = new ArrayList<>(dtoList.size());
        for (CategoryAttributeDTO dto : dtoList) {
            result.add(toVO(dto));
        }
        return result;
    }

    private static CategoryAttributeDTO.CategoryAttributeValueDTO toDTO(
            CategoryAttributeVO.CategoryAttributeValueVO vvo) {
        if (vvo == null) {
            return null;
        }
        return CategoryAttributeDTO.CategoryAttributeValueDTO.builder()
                .id(vvo.getId())
                .value(vvo.getValue())
                .imageUrl(vvo.getImageUrl())
                .sortOrder(vvo.getSortOrder())
                .build();
    }

    private static CategoryAttributeVO.CategoryAttributeValueVO toVO(
            CategoryAttributeDTO.CategoryAttributeValueDTO vd) {
        if (vd == null) {
            return null;
        }
        CategoryAttributeVO.CategoryAttributeValueVO vvo =
                new CategoryAttributeVO.CategoryAttributeValueVO();
        vvo.setId(vd.getId());
        vvo.setValue(vd.getValue());
        vvo.setImageUrl(vd.getImageUrl());
        vvo.setSortOrder(vd.getSortOrder());
        return vvo;
    }
}