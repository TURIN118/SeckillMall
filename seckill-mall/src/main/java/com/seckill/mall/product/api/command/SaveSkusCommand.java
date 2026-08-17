package com.seckill.mall.product.api.command;

import com.seckill.mall.product.api.dto.SkuSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量保存 SKU 命令。
 *
 * <p>业务语义：批量保存商品 SKU（先逻辑删除旧 SKU，再插入新 SKU）。
 *
 * <p>原方法：{@code ProductSkuService.saveSkus(Long, List<ProductSkuDTO>)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveSkusCommand {

    /** 商品 ID（必填） */
    private Long productId;

    /** SKU 列表（非空，必填） */
    private List<SkuSnapshot> skus;
}