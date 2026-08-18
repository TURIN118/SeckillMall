package com.seckill.mall.payment.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 充值卡分页查询条件（后台 listPage）。
 *
 * <p>原方法：{@code RechargeCardService.listPage(Integer pageNum, Integer pageSize, String batchNo, String status)}
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCardQuery {

    /** 页码（默认1） */
    private Integer pageNum;

    /** 每页大小（默认10） */
    private Integer pageSize;

    /** 批次号筛选（可空） */
    private String batchNo;

    /** 状态筛选（可空）：UNUSED/USED/DISABLED */
    private String status;
}