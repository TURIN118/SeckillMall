package com.seckill.mall.service;

import com.seckill.mall.dto.ProductAttributeDTO;
import com.seckill.mall.vo.ProductAttributeVO;

import java.util.List;

/**
 * 商品属性服务接口
 * <p>
 * 维护商品维度的属性定义（如「颜色」「版本」）及其属性值，
 * 供商品详情接口返回、商品创建/更新时保存属性。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductAttributeService.java
 * 邮箱：nj651217@163.com
 */
public interface ProductAttributeService {

    /**
     * 查询商品的所有属性及其值（按 sortOrder 升序）
     *
     * @param productId 商品 ID
     * @return 属性视图列表（含属性值）
     */
    List<ProductAttributeVO> listByProductId(Long productId);

    /**
     * 批量保存商品的属性及其值（创建 / 更新商品时调用）
     * <p>
     * 采用「先逻辑删除旧属性，再插入新属性」策略，简化实现
     *
     * @param productId  商品 ID
     * @param attributes 属性 DTO 列表
     */
    void saveAttributes(Long productId, List<ProductAttributeDTO> attributes);

    /**
     * 逻辑删除商品的所有属性及其值
     *
     * @param productId 商品 ID
     */
    void deleteByProductId(Long productId);
}