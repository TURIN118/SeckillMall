package com.seckill.mall.product.api;

import com.seckill.mall.product.api.command.SaveAttributesCommand;
import com.seckill.mall.product.api.dto.AttributeDTO;

import java.util.List;

/**
 * Product 模块属性能力 API。
 *
 * <p>对外暴露商品属性及属性值维护契约。
 *
 * @author wnj
 * @since Phase P.2
 */
public interface AttributeApi {

    /**
     * 查询商品的所有属性及其值（按 sortOrder 升序）。
     *
     * @param productId 商品 ID
     * @return 属性 DTO 列表
     */
    List<AttributeDTO> listAttributesByProductId(Long productId);

    /**
     * 批量保存商品的属性及其值（先逻辑删除旧属性，再插入新属性）。
     *
     * @param command 保存属性命令
     * @throws com.seckill.mall.exception.BusinessException {@code PARAM_ERROR}
     */
    void saveAttributes(SaveAttributesCommand command);

    /**
     * 逻辑删除商品的所有属性及其值。
     *
     * @param productId 商品 ID
     */
    void deleteAttributesByProductId(Long productId);
}