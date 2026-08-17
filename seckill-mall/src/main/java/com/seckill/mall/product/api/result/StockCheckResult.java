package com.seckill.mall.product.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存校验结果（预留）。
 *
 * <p>用于库存扣减前的校验出参。
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckResult {

    /** 库存是否充足 */
    private boolean available;

    /** 当前库存 */
    private Integer currentStock;
}