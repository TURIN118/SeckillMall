package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.dto.ProductUpdateRequest;
import com.seckill.mall.product.infrastructure.entity.Product;
import com.seckill.mall.vo.ProductVO;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductService.java
 * 邮箱：nj651217@163.com
 */
public interface ProductService {

    PageResult<ProductVO> listProducts(ProductQueryRequest req);

    ProductVO getProductDetail(Long id);

    ProductVO createProduct(ProductCreateRequest req);

    ProductVO updateProduct(Long id, ProductUpdateRequest req);

    void deleteProduct(Long id);

    /**
     * 检查商品是否存在。
     *
     * @param id 商品 ID
     * @return true 表示存在
     */
    boolean existsById(Long id);

    /**
     * 根据 ID 查询商品实体（跨模块只读访问）。
     *
     * @param id 商品 ID
     * @return 商品实体，不存在时返回 null
     */
    Product getProductById(Long id);

    /**
     * 根据 ID 列表批量查询商品实体（跨模块只读访问）。
     *
     * @param ids 商品 ID 列表
     * @return 商品实体列表
     */
    List<Product> getProductsByIds(List<Long> ids);

    /**
     * Phase 14：商品总数（封装 productMapper.selectCount(null)，消除跨模块 Mapper 依赖）。
     *
     * @return 商品总数
     */
    long countAll();

    /**
     * Phase 15：递增/递减商品加购计数（冗余计数维护）。
     * <p>
     * 使用 {@code setSql} 直接执行 {@code cart_count = cart_count + delta}，
     * 避免并发下的覆盖更新。{@code @TableLogic} 自动追加 {@code is_deleted=0} 条件。
     *
     * @param productId 商品 ID
     * @param delta     变化量（+1 或 -1）
     */
    void updateCartCount(Long productId, int delta);

    /**
     * Phase 15：递增/递减商品收藏计数（冗余计数维护）。
     * <p>
     * 使用 {@code setSql} 直接执行 {@code favorite_count = favorite_count + delta}，
     * 避免并发下的覆盖更新。{@code @TableLogic} 自动追加 {@code is_deleted=0} 条件。
     *
     * @param productId 商品 ID
     * @param delta     变化量（+1 或 -1）
     */
    void updateFavoriteCount(Long productId, int delta);
}
