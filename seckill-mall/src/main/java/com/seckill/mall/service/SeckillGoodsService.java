package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.vo.SeckillGoodsVO;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillGoodsService.java
 * 邮箱：nj651217@163.com
 */
public interface SeckillGoodsService {

    PageResult<SeckillGoodsVO> listSeckill(String status, Long categoryId, Integer pageNum, Integer pageSize);

    SeckillGoodsVO getSeckillDetail(Long seckillId);

    SeckillGoodsVO createSeckill(SeckillCreateRequest req);

    SeckillGoodsVO updateSeckill(Long id, SeckillCreateRequest req);

    void cancelSeckill(Long id);

    Integer getStock(Long seckillId);

    void preheatSeckill(Long seckillId);
}
