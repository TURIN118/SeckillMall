package com.seckill.mall.product.api.command;

import com.seckill.mall.product.api.dto.AttributeDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量保存属性命令。
 *
 * <p>业务语义：批量保存商品的属性及其值（先逻辑删除旧属性，再插入新属性）。
 *
 * <p>原方法：{@code ProductAttributeService.saveAttributes(Long, List<ProductAttributeDTO>)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAttributesCommand {

    /** 商品 ID（必填） */
    private Long productId;

    /** 属性及属性值列表（必填） */
    private List<AttributeDTO> attributes;
}