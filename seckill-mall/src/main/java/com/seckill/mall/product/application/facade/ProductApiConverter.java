package com.seckill.mall.product.application.facade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.dto.ProductAttributeDTO;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.dto.ProductSkuDTO;
import com.seckill.mall.dto.ProductUpdateRequest;
import com.seckill.mall.product.api.command.CreateProductCommand;
import com.seckill.mall.product.api.command.UpdateProductCommand;
import com.seckill.mall.product.api.dto.AttributeDTO;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.product.api.dto.ProductSummaryDTO;
import com.seckill.mall.product.api.dto.ReviewDTO;
import com.seckill.mall.product.api.dto.SkuSnapshot;
import com.seckill.mall.product.api.query.ProductListQuery;
import com.seckill.mall.product.api.result.ProductDetailResult;
import com.seckill.mall.product.domain.AttributeType;
import com.seckill.mall.product.domain.ProductStatus;
import com.seckill.mall.product.infrastructure.entity.Product;
import com.seckill.mall.product.infrastructure.entity.ProductAttribute;
import com.seckill.mall.product.infrastructure.entity.ProductAttributeValue;
import com.seckill.mall.product.infrastructure.entity.ProductReview;
import com.seckill.mall.product.infrastructure.entity.ProductSku;
import com.seckill.mall.vo.ProductAttributeVO;
import com.seckill.mall.vo.ProductReviewVO;
import com.seckill.mall.vo.ProductSkuVO;
import com.seckill.mall.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Product API 转换辅助类。
 *
 * <p>集中存放旧 VO/Entity 与新 API 层 DTO/Result/Snapshot 之间的转换方法，
 * 供 ApplicationService 调用。所有方法均为无状态静态方法，
 * 标注 {@code @Component} 仅为便于未来扩展为 Bean 注入方式。
 *
 * <p>转换原则：
 * <ul>
 *     <li>VO → Result/DTO：提取核心字段，丢弃前端展示专用字段</li>
 *     <li>Entity → Snapshot：仅提取跨模块传递所需字段，避免暴露 Entity</li>
 *     <li>Command → Request：API 层 Command → 旧 DTO Request，字段一一对应</li>
 *     <li>Query → Request：API 层 Query → 旧 DTO Request，字段一一对应</li>
 * </ul>
 *
 * @author wnj
 * @since Phase P.4-A
 */
@Slf4j
@Component
public class ProductApiConverter {

    /** 静态 ObjectMapper，用于 images 字段 List&lt;String&gt; ↔ JSON String 转换 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ============================================================
    // Product Entity → ProductSnapshot 转换（跨模块只读快照）
    // ============================================================

    /**
     * 将 {@link Product} Entity 转换为 {@link ProductSnapshot}。
     *
     * <p>仅提取跨模块传递所需字段，避免暴露 Entity。
     *
     * @param entity 商品 Entity
     * @return 商品快照
     */
    public static ProductSnapshot toSnapshot(Product entity) {
        if (entity == null) {
            return null;
        }
        return ProductSnapshot.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .originalPrice(entity.getOriginalPrice())
                .stock(entity.getStock())
                .minPrice(entity.getMinPrice())
                .maxPrice(entity.getMaxPrice())
                .totalStock(entity.getTotalStock())
                .salesCount(entity.getSalesCount())
                .cartCount(entity.getCartCount())
                .favoriteCount(entity.getFavoriteCount())
                .categoryId(entity.getCategoryId())
                .images(entity.getImages())
                .mainImage(entity.getMainImage())
                .detailHtml(entity.getDetailHtml())
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    /**
     * 将 {@link Product} Entity 列表转换为 {@link ProductSnapshot} 列表。
     *
     * @param entities 商品 Entity 列表
     * @return 商品快照列表
     */
    public static List<ProductSnapshot> toSnapshotList(List<Product> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ProductApiConverter::toSnapshot)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ProductVO → ProductSummaryDTO / ProductDetailResult 转换
    // ============================================================

    /**
     * 将 {@link ProductVO} 转换为 {@link ProductSummaryDTO}。
     *
     * <p>提取列表展示所需摘要字段。mainImage 取 images 列表首张。
     *
     * @param vo 商品 VO
     * @return 商品摘要 DTO
     */
    public static ProductSummaryDTO toSummaryDTO(ProductVO vo) {
        if (vo == null) {
            return null;
        }
        return ProductSummaryDTO.builder()
                .id(vo.getId())
                .name(vo.getProductName())
                .mainImage(extractMainImage(vo.getImages()))
                .originalPrice(vo.getOriginalPrice())
                .minPrice(vo.getMinPrice())
                .maxPrice(vo.getMaxPrice())
                .totalStock(vo.getTotalStock())
                .salesCount(vo.getSalesCount())
                .status(vo.getStatus() != null ? vo.getStatus().getCode() : null)
                .categoryId(vo.getCategoryId())
                .createTime(vo.getCreateTime())
                .build();
    }

    /**
     * 将 {@link ProductVO} 转换为 {@link ProductDetailResult}。
     *
     * <p>全字段映射，包含 SKU 列表与属性列表。
     *
     * @param vo 商品 VO
     * @return 商品详情结果
     */
    public static ProductDetailResult toDetailResult(ProductVO vo) {
        if (vo == null) {
            return null;
        }
        return ProductDetailResult.builder()
                .id(vo.getId())
                .name(vo.getProductName())
                .description(vo.getDescription())
                .originalPrice(vo.getOriginalPrice())
                .stock(vo.getStock())
                .minPrice(vo.getMinPrice())
                .maxPrice(vo.getMaxPrice())
                .totalStock(vo.getTotalStock())
                .salesCount(vo.getSalesCount())
                .cartCount(null)
                .favoriteCount(null)
                .categoryId(vo.getCategoryId())
                .images(imagesToString(vo.getImages()))
                .mainImage(extractMainImage(vo.getImages()))
                .detailHtml(vo.getDetailHtml())
                .status(vo.getStatus() != null ? vo.getStatus().getCode() : null)
                .skus(toSkuSnapshotListFromVO(vo.getSkus()))
                .attributes(toAttributeDTOListFromVO(vo.getAttributes()))
                .createTime(vo.getCreateTime())
                .updateTime(null)
                .build();
    }

    // ============================================================
    // ProductSku Entity → SkuSnapshot 转换
    // ============================================================

    /**
     * 将 {@link ProductSku} Entity 转换为 {@link SkuSnapshot}。
     *
     * @param entity SKU Entity
     * @return SKU 快照
     */
    public static SkuSnapshot toSkuSnapshot(ProductSku entity) {
        if (entity == null) {
            return null;
        }
        return SkuSnapshot.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .skuCode(entity.getSkuCode())
                .price(entity.getPrice())
                .stock(entity.getStock())
                .mainImage(entity.getMainImage())
                .attributes(entity.getAttributes())
                .status(entity.getStatus())
                .build();
    }

    /**
     * 将 {@link ProductSku} Entity 列表转换为 {@link SkuSnapshot} 列表。
     *
     * @param entities SKU Entity 列表
     * @return SKU 快照列表
     */
    public static List<SkuSnapshot> toSkuSnapshotList(List<ProductSku> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ProductApiConverter::toSkuSnapshot)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ProductSkuVO → SkuSnapshot 转换
    // ============================================================

    /**
     * 将 {@link ProductSkuVO} 转换为 {@link SkuSnapshot}。
     *
     * @param vo SKU VO
     * @return SKU 快照
     */
    public static SkuSnapshot toSkuSnapshotFromVO(ProductSkuVO vo) {
        if (vo == null) {
            return null;
        }
        return SkuSnapshot.builder()
                .id(vo.getId())
                .skuCode(vo.getSkuCode())
                .price(vo.getPrice())
                .stock(vo.getStock())
                .mainImage(vo.getMainImage())
                .attributes(vo.getAttributes())
                .status(vo.getStatus())
                .build();
    }

    /**
     * 将 {@link ProductSkuVO} 列表转换为 {@link SkuSnapshot} 列表。
     *
     * @param voList SKU VO 列表
     * @return SKU 快照列表
     */
    public static List<SkuSnapshot> toSkuSnapshotListFromVO(List<ProductSkuVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(ProductApiConverter::toSkuSnapshotFromVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ProductReview Entity → ReviewDTO 转换
    // ============================================================

    /**
     * 将 {@link ProductReview} Entity 转换为 {@link ReviewDTO}。
     *
     * @param entity 评论 Entity
     * @return 评论 DTO
     */
    public static ReviewDTO toReviewDTO(ProductReview entity) {
        if (entity == null) {
            return null;
        }
        return ReviewDTO.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .skuId(entity.getSkuId())
                .skuAttributes(entity.getSkuAttributes())
                .userId(entity.getUserId())
                .orderId(entity.getOrderId())
                .content(entity.getContent())
                .rating(entity.getRating())
                .images(entity.getImages())
                .status(entity.getStatus())
                .replyContent(entity.getReplyContent())
                .replyTime(entity.getReplyTime())
                .createTime(entity.getCreateTime())
                .build();
    }

    // ============================================================
    // ProductReviewVO → ReviewDTO 转换
    // ============================================================

    /**
     * 将 {@link ProductReviewVO} 转换为 {@link ReviewDTO}。
     *
     * <p>images 字段：VO 为 List&lt;String&gt;，DTO 为 JSON String，需序列化。
     *
     * @param vo 评论 VO
     * @return 评论 DTO
     */
    public static ReviewDTO toReviewDTOFromVO(ProductReviewVO vo) {
        if (vo == null) {
            return null;
        }
        return ReviewDTO.builder()
                .id(vo.getId())
                .productId(vo.getProductId())
                .skuId(vo.getSkuId())
                .skuAttributes(vo.getSkuAttributes())
                .userId(vo.getUserId())
                .orderId(vo.getOrderId())
                .content(vo.getContent())
                .rating(vo.getRating())
                .images(imagesToString(vo.getImages()))
                .status(vo.getStatus())
                .replyContent(vo.getReplyContent())
                .replyTime(vo.getReplyTime())
                .createTime(vo.getCreateTime())
                .build();
    }

    /**
     * 将 {@link ProductReviewVO} 列表转换为 {@link ReviewDTO} 列表。
     *
     * @param voList 评论 VO 列表
     * @return 评论 DTO 列表
     */
    public static List<ReviewDTO> toReviewDTOListFromVO(List<ProductReviewVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(ProductApiConverter::toReviewDTOFromVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ProductAttribute Entity → AttributeDTO 转换
    // ============================================================

    /**
     * 将 {@link ProductAttribute} Entity 转换为 {@link AttributeDTO}。
     *
     * <p>注意：Entity 不包含属性值列表，values 设为 null。
     *
     * @param entity 属性 Entity
     * @return 属性 DTO
     */
    public static AttributeDTO toAttributeDTO(ProductAttribute entity) {
        if (entity == null) {
            return null;
        }
        return AttributeDTO.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .categoryAttributeId(entity.getCategoryAttributeId())
                .name(entity.getName())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .sortOrder(entity.getSortOrder())
                .values(null)
                .build();
    }

    // ============================================================
    // ProductAttributeVO → AttributeDTO 转换
    // ============================================================

    /**
     * 将 {@link ProductAttributeVO} 转换为 {@link AttributeDTO}。
     *
     * @param vo 属性 VO
     * @return 属性 DTO
     */
    public static AttributeDTO toAttributeDTOFromVO(ProductAttributeVO vo) {
        if (vo == null) {
            return null;
        }
        return AttributeDTO.builder()
                .id(vo.getId())
                .categoryAttributeId(vo.getCategoryAttributeId())
                .name(vo.getName())
                .type(vo.getType())
                .sortOrder(vo.getSortOrder())
                .values(toAttributeValueDTOListFromVO(vo.getValues()))
                .build();
    }

    /**
     * 将 {@link ProductAttributeVO} 列表转换为 {@link AttributeDTO} 列表。
     *
     * @param voList 属性 VO 列表
     * @return 属性 DTO 列表
     */
    public static List<AttributeDTO> toAttributeDTOListFromVO(List<ProductAttributeVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(ProductApiConverter::toAttributeDTOFromVO)
                .collect(Collectors.toList());
    }

    /**
     * 将 {@link ProductAttributeVO.AttributeValueVO} 列表转换为
     * {@link AttributeDTO.AttributeValueDTO} 列表。
     *
     * @param voList 属性值 VO 列表
     * @return 属性值 DTO 列表
     */
    private static List<AttributeDTO.AttributeValueDTO> toAttributeValueDTOListFromVO(
            List<ProductAttributeVO.AttributeValueVO> voList) {
        if (voList == null) {
            return null;
        }
        return voList.stream()
                .map(v -> AttributeDTO.AttributeValueDTO.builder()
                        .id(v.getId())
                        .value(v.getValue())
                        .imageUrl(v.getImageUrl())
                        .sortOrder(v.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================================
    // ProductListQuery → ProductQueryRequest 转换（Query → 旧 DTO）
    // ============================================================

    /**
     * 将 API 层 {@link ProductListQuery} 转换为旧 {@link ProductQueryRequest}。
     *
     * @param query API 层商品列表查询条件
     * @return 旧商品查询请求
     */
    public static ProductQueryRequest toProductQueryRequest(ProductListQuery query) {
        if (query == null) {
            return null;
        }
        ProductQueryRequest req = new ProductQueryRequest();
        req.setCategoryId(query.getCategoryId());
        req.setStatus(query.getStatus());
        req.setKeyword(query.getKeyword());
        req.setPageNum(query.getPageNum() != null ? query.getPageNum() : 1);
        req.setPageSize(query.getPageSize() != null ? query.getPageSize() : 10);
        return req;
    }

    // ============================================================
    // CreateProductCommand → ProductCreateRequest 转换（Command → 旧 DTO）
    // ============================================================

    /**
     * 将 API 层 {@link CreateProductCommand} 转换为旧 {@link ProductCreateRequest}。
     *
     * @param cmd 新增商品命令
     * @return 旧商品创建请求
     */
    public static ProductCreateRequest toProductCreateRequest(CreateProductCommand cmd) {
        if (cmd == null) {
            return null;
        }
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductName(cmd.getName());
        req.setDescription(cmd.getDescription());
        req.setOriginalPrice(cmd.getOriginalPrice());
        req.setStock(cmd.getStock());
        req.setCategoryId(cmd.getCategoryId());
        req.setImages(stringToImages(cmd.getImages()));
        req.setDetailHtml(cmd.getDetailHtml());
        req.setStatus(parseProductStatus(cmd.getStatus()));
        req.setSkus(toProductSkuDTOList(cmd.getSkus()));
        req.setAttributes(toProductAttributeDTOList(cmd.getAttributes()));
        return req;
    }

    // ============================================================
    // UpdateProductCommand → ProductUpdateRequest 转换（Command → 旧 DTO）
    // ============================================================

    /**
     * 将 API 层 {@link UpdateProductCommand} 转换为旧 {@link ProductUpdateRequest}。
     *
     * @param cmd 编辑商品命令
     * @return 旧商品更新请求
     */
    public static ProductUpdateRequest toProductUpdateRequest(UpdateProductCommand cmd) {
        if (cmd == null) {
            return null;
        }
        ProductUpdateRequest req = new ProductUpdateRequest();
        req.setProductName(cmd.getName());
        req.setDescription(cmd.getDescription());
        req.setOriginalPrice(cmd.getOriginalPrice());
        req.setStock(cmd.getStock());
        req.setCategoryId(cmd.getCategoryId());
        req.setImages(stringToImages(cmd.getImages()));
        req.setDetailHtml(cmd.getDetailHtml());
        req.setStatus(parseProductStatus(cmd.getStatus()));
        req.setSkus(toProductSkuDTOList(cmd.getSkus()));
        req.setAttributes(toProductAttributeDTOList(cmd.getAttributes()));
        return req;
    }

    // ============================================================
    // SkuSnapshot → ProductSkuDTO 转换（DTO → 旧 DTO）
    // ============================================================

    /**
     * 将 {@link SkuSnapshot} 转换为旧 {@link ProductSkuDTO}。
     *
     * @param snapshot SKU 快照
     * @return 旧 SKU DTO
     */
    public static ProductSkuDTO toProductSkuDTO(SkuSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        ProductSkuDTO dto = new ProductSkuDTO();
        dto.setId(snapshot.getId());
        dto.setSkuCode(snapshot.getSkuCode());
        dto.setPrice(snapshot.getPrice());
        dto.setStock(snapshot.getStock());
        dto.setMainImage(snapshot.getMainImage());
        dto.setAttributes(snapshot.getAttributes());
        dto.setStatus(snapshot.getStatus());
        return dto;
    }

    /**
     * 将 {@link SkuSnapshot} 列表转换为旧 {@link ProductSkuDTO} 列表。
     *
     * @param snapshots SKU 快照列表
     * @return 旧 SKU DTO 列表
     */
    public static List<ProductSkuDTO> toProductSkuDTOList(List<SkuSnapshot> snapshots) {
        if (snapshots == null) {
            return null;
        }
        return snapshots.stream()
                .map(ProductApiConverter::toProductSkuDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // AttributeDTO → ProductAttributeDTO 转换（DTO → 旧 DTO）
    // ============================================================

    /**
     * 将 {@link AttributeDTO} 转换为旧 {@link ProductAttributeDTO}。
     *
     * @param dto 属性 DTO
     * @return 旧属性 DTO
     */
    public static ProductAttributeDTO toProductAttributeDTO(AttributeDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductAttributeDTO result = new ProductAttributeDTO();
        result.setId(dto.getId());
        result.setCategoryAttributeId(dto.getCategoryAttributeId());
        result.setName(dto.getName());
        result.setType(dto.getType());
        result.setSortOrder(dto.getSortOrder());
        result.setValues(toOldAttributeValueDTOList(dto.getValues()));
        return result;
    }

    /**
     * 将 {@link AttributeDTO} 列表转换为旧 {@link ProductAttributeDTO} 列表。
     *
     * @param dtos 属性 DTO 列表
     * @return 旧属性 DTO 列表
     */
    public static List<ProductAttributeDTO> toProductAttributeDTOList(List<AttributeDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(ProductApiConverter::toProductAttributeDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将 {@link AttributeDTO.AttributeValueDTO} 列表转换为旧
     * {@link ProductAttributeDTO.AttributeValueDTO} 列表。
     *
     * @param dtos 属性值 DTO 列表
     * @return 旧属性值 DTO 列表
     */
    private static List<ProductAttributeDTO.AttributeValueDTO> toOldAttributeValueDTOList(
            List<AttributeDTO.AttributeValueDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(d -> {
                    ProductAttributeDTO.AttributeValueDTO v = new ProductAttributeDTO.AttributeValueDTO();
                    v.setId(d.getId());
                    v.setValue(d.getValue());
                    v.setImageUrl(d.getImageUrl());
                    v.setSortOrder(d.getSortOrder());
                    return v;
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 从图片列表提取主图（首张）。
     *
     * @param images 图片列表
     * @return 主图 URL，列表为空返回 null
     */
    private static String extractMainImage(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.get(0);
    }

    /**
     * 将 List&lt;String&gt; 序列化为 JSON 字符串。
     *
     * @param images 图片列表
     * @return JSON 字符串，失败返回 null
     */
    private static String imagesToString(List<String> images) {
        if (images == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(images);
        } catch (Exception e) {
            log.warn("序列化 images 列表失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为 List&lt;String&gt;。
     *
     * @param json JSON 字符串
     * @return 图片列表，失败返回空列表
     */
    private static List<String> stringToImages(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("反序列化 images JSON 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 将状态码字符串解析为 {@link ProductStatus} 枚举。
     *
     * @param code 状态码（ON_SALE/OFF_SHELF）
     * @return 商品状态枚举，null 返回 null
     */
    private static ProductStatus parseProductStatus(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return ProductStatus.fromCode(code);
    }
}