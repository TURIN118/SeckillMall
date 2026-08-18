package com.seckill.mall.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀场次 DTO，替代 SeckillActivityVO 跨模块与跨层传递。
 *
 * <p>包含场次完整字段（核心 + 商品列表），供 Application 层返回与 Controller 层转回 VO 使用。
 *
 * <p>来源映射：
 * <ul>
 *     <li>SeckillActivityVO → SeckillActivityDTO（{@code SeckillApiConverter.toDTO}）</li>
 * </ul>
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 5.1 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillActivityDTO {

    /** 场次 ID */
    private Long id;

    /** 场次名称 */
    private String name;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 状态 */
    private Integer status;

    /** 场次下商品列表 */
    private List<SeckillGoodsDTO> goodsList;
}