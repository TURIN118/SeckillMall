package com.seckill.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillActivityVO.java
 * 邮箱：nj651217@163.com
 * <p>
 * 秒杀场次 VO，含该场次下的所有秒杀商品列表。
 */
@Data
public class SeckillActivityVO {

    private Long id;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 0=待开始 1=进行中 2=已结束 */
    private Integer status;

    private Integer perLimit;

    private String description;

    private List<String> images;

    private LocalDateTime createTime;

    /** 该场次下的秒杀商品列表 */
    private List<SeckillGoodsVO> goodsList;
}