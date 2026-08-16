package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.dto.ProductUpdateRequest;
import com.seckill.mall.vo.ProductVO;

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
}
