package com.seckill.mall.product.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.product.api.command.CreateProductCommand;
import com.seckill.mall.product.api.command.UpdateCartCountCommand;
import com.seckill.mall.product.api.command.UpdateFavoriteCountCommand;
import com.seckill.mall.product.api.command.UpdateProductCommand;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.product.api.dto.ProductSummaryDTO;
import com.seckill.mall.product.api.query.ProductListQuery;
import com.seckill.mall.product.api.result.ProductDetailResult;

import java.util.List;

/**
 * Product 模块商品业务能力 API。
 *
 * <p>对外暴露商品 CRUD、跨模块只读快照查询、冗余计数维护等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * @author wnj
 * @since Phase P.2
 */
public interface ProductApi {

    /**
     * 商品列表分页查询。
     *
     * @param query 商品列表查询条件
     * @return 分页结果
     */
    PageResult<ProductSummaryDTO> listProducts(ProductListQuery query);

    /**
     * 查询商品详情（含 SKU 列表 + 属性列表 + 价格区间）。
     *
     * @param id 商品 ID
     * @return 商品详情结果
     * @throws com.seckill.mall.exception.BusinessException {@code PRODUCT_NOT_FOUND}
     */
    ProductDetailResult getProductDetail(Long id);

    /**
     * 新增商品。
     *
     * @param command 新增商品命令
     * @return 商品详情结果
     * @throws com.seckill.mall.exception.BusinessException {@code PARAM_ERROR}、{@code CATEGORY_NOT_FOUND}
     */
    ProductDetailResult createProduct(CreateProductCommand command);

    /**
     * 编辑商品。
     *
     * @param command 编辑商品命令
     * @return 商品详情结果
     * @throws com.seckill.mall.exception.BusinessException {@code PRODUCT_NOT_FOUND}、{@code PARAM_ERROR}
     */
    ProductDetailResult updateProduct(UpdateProductCommand command);

    /**
     * 逻辑删除商品。
     *
     * @param id 商品 ID
     * @throws com.seckill.mall.exception.BusinessException {@code PRODUCT_NOT_FOUND}
     */
    void deleteProduct(Long id);

    /**
     * 检查商品是否存在（未逻辑删除）。
     *
     * @param id 商品 ID
     * @return {@code true} 存在
     */
    boolean existsById(Long id);

    /**
     * 根据 ID 查询商品快照（跨模块只读访问）。
     *
     * @param id 商品 ID
     * @return 商品快照（不存在返回 null）
     */
    ProductSnapshot getProductById(Long id);

    /**
     * 根据 ID 列表批量查询商品快照。
     *
     * @param ids 商品 ID 列表
     * @return 商品快照列表
     */
    List<ProductSnapshot> getProductsByIds(List<Long> ids);

    /**
     * 查询商品总数。
     *
     * @return 商品总数
     */
    long countAll();

    /**
     * 递增/递减商品加购计数。
     *
     * @param command 更新加购计数命令
     */
    void updateCartCount(UpdateCartCountCommand command);

    /**
     * 递增/递减商品收藏计数。
     *
     * @param command 更新收藏计数命令
     */
    void updateFavoriteCount(UpdateFavoriteCountCommand command);
}