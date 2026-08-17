package com.seckill.mall.product.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.product.infrastructure.entity.ProductSku;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品 SKU Mapper
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSkuMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 查询商品所有启用 SKU（按 id 升序）
     */
    List<ProductSku> selectEnabledByProductId(@Param("productId") Long productId);

    /**
     * 【已废弃 / 不推荐】按 attributes JSON 精确匹配 SKU。
     *
     * 建议4 已落实：加购 / 立即购买时前端直接传 skuId（前端在用户选完属性后
     * 从内存 skus 数组查找匹配的 skuId），后端无需通过 JSON 匹配查 SKU。
     *
     * 保留该方法仅供调试 / 兼容旧接口使用，新代码不应调用。
     * 性能问题：JSON 字段精确匹配不走索引，数据量大时慢。
     */
    @Deprecated
    ProductSku selectByAttributes(@Param("productId") Long productId,
                                  @Param("attributesJson") String attributesJson);

    /**
     * 物理删除指定商品的所有 SKU（绕过 MyBatis-Plus 逻辑删除）。
     * <p>
     * 用于编辑商品保存 SKU 前清理旧 SKU：
     * 唯一索引 uk_product_sku_code(product_id, sku_code) 不包含 is_deleted 字段，
     * 若使用逻辑删除，属性组合不变时 sku_code 重复会导致唯一键冲突。
     * 编辑商品时 SKU 重新生成，旧 SKU 不需要保留，故使用物理删除。
     *
     * @param productId 商品 ID
     * @return 受影响行数
     */
    @Delete("DELETE FROM t_product_sku WHERE product_id = #{productId}")
    int physicalDeleteByProductId(@Param("productId") Long productId);
}