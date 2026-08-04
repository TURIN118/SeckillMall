package com.seckill.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.entity.Category;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.mapper.CategoryMapper;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.service.CategoryService;
import com.seckill.mall.service.impl.ProductServiceImpl;
import com.seckill.mall.vo.ProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String CACHE_KEY = "seckill:goods:1";

    @Mock
    private ProductMapper productMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock rLock;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product buildProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("iPhone");
        p.setCategoryId(101L);
        p.setOriginalPrice(new BigDecimal("9999.00"));
        p.setStock(100);
        p.setSalesCount(10);
        p.setStatus(ProductStatus.ON_SALE);
        p.setImages("[\"img1\"]");
        return p;
    }

    private Category buildCategory() {
        Category c = new Category();
        c.setId(101L);
        c.setName("智能手机");
        c.setStatus(1);
        return c;
    }

    @Test
    @DisplayName("listProducts：分页查询返回 VO 列表与总数")
    void listProducts_shouldReturnPagedResult() {
        // given
        ProductQueryRequest req = new ProductQueryRequest();
        req.setPageNum(1);
        req.setPageSize(10);

        Product product = buildProduct();
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(List.of(product));
        page.setTotal(1L);
        // 未指定 status 时传 null，返回所有商品(含下架)
        given(productMapper.selectProductPage(any(Page.class), any(), any(), any(), isNull(), any(), any(), any(), any()))
                .willReturn(page);
        given(categoryMapper.selectBatchIds(List.of(101L))).willReturn(List.of(buildCategory()));

        // when
        PageResult<ProductVO> result = productService.listProducts(req);

        // then
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList().get(0).getProductName()).isEqualTo("iPhone");
        assertThat(result.getList().get(0).getCategoryName()).isEqualTo("智能手机");
    }

    @Test
    @DisplayName("listProducts：空结果集直接返回空分页（不查分类）")
    void listProducts_shouldReturnEmptyWithoutCategoryLookup() {
        // given
        ProductQueryRequest req = new ProductQueryRequest();
        Page<Product> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        // 未指定 status 时传 null，返回所有商品(含下架)
        given(productMapper.selectProductPage(any(Page.class), any(), any(), any(), isNull(), any(), any(), any(), any()))
                .willReturn(emptyPage);

        // when
        PageResult<ProductVO> result = productService.listProducts(req);

        // then
        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        then(categoryMapper).should(never()).selectBatchIds(any());
    }

    @Test
    @DisplayName("listProducts：指定 status=ON_SALE 时按上架筛选")
    void listProducts_shouldFilterByStatusWhenSpecified() {
        // given
        ProductQueryRequest req = new ProductQueryRequest();
        req.setPageNum(1);
        req.setPageSize(10);
        req.setStatus("ON_SALE");

        Product product = buildProduct();
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(List.of(product));
        page.setTotal(1L);
        // 指定 status 时应传对应枚举值
        given(productMapper.selectProductPage(any(Page.class), any(), any(), any(), eq(ProductStatus.ON_SALE), any(), any(), any(), any()))
                .willReturn(page);
        given(categoryMapper.selectBatchIds(List.of(101L))).willReturn(List.of(buildCategory()));

        // when
        PageResult<ProductVO> result = productService.listProducts(req);

        // then
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("listProducts：指定 status=OFF_SHELF 时按下架筛选")
    void listProducts_shouldFilterByOffShelfWhenSpecified() {
        // given
        ProductQueryRequest req = new ProductQueryRequest();
        req.setPageNum(1);
        req.setPageSize(10);
        req.setStatus("OFF_SHELF");

        Page<Product> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        // 指定 OFF_SHELF 时应传 ProductStatus.OFF_SHELF
        given(productMapper.selectProductPage(any(Page.class), any(), any(), any(), eq(ProductStatus.OFF_SHELF), any(), any(), any(), any()))
                .willReturn(emptyPage);

        // when
        PageResult<ProductVO> result = productService.listProducts(req);

        // then
        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    @DisplayName("getProductDetail：缓存命中真实数据时直接返回，不查 DB")
    void getProductDetail_shouldReturnFromCache() throws Exception {
        // given
        ProductVO cached = new ProductVO();
        cached.setId(1L);
        cached.setProductName("iPhone");
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn("{\"id\":1}");
        given(objectMapper.readValue("{\"id\":1}", ProductVO.class)).willReturn(cached);

        // when
        ProductVO vo = productService.getProductDetail(1L);

        // then
        assertThat(vo.getProductName()).isEqualTo("iPhone");
        then(productMapper).should(never()).selectById(anyLong());
        then(redissonClient).should(never()).getLock(anyString());
    }

    @Test
    @DisplayName("getProductDetail：缓存空值标记时抛 PRODUCT_NOT_FOUND")
    void getProductDetail_shouldThrowWhenCacheIsNullMarker() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn("NULL");

        // when / then
        assertThatThrownBy(() -> productService.getProductDetail(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        then(productMapper).should(never()).selectById(anyLong());
    }

    @Test
    @DisplayName("getProductDetail：缓存未命中时查 DB 并回写缓存")
    void getProductDetail_shouldLoadFromDbAndFillCache() throws Exception {
        // given
        Product product = buildProduct();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        // 首次查询与 Double Check 均未命中
        given(valueOperations.get(CACHE_KEY)).willReturn(null);
        given(redissonClient.getLock("lock:goods:1")).willReturn(rLock);
        given(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).willReturn(true);
        given(productMapper.selectById(1L)).willReturn(product);
        given(categoryMapper.selectBatchIds(List.of(101L))).willReturn(List.of(buildCategory()));
        given(objectMapper.writeValueAsString(any(ProductVO.class))).willReturn("{}");

        // when
        ProductVO vo = productService.getProductDetail(1L);

        // then
        assertThat(vo.getProductName()).isEqualTo("iPhone");
        then(productMapper).should().selectById(1L);
        then(valueOperations).should().set(eq(CACHE_KEY), eq("{}"), anyLong(), eq(TimeUnit.SECONDS));
        then(rLock).should().unlock();
    }

    @Test
    @DisplayName("getProductDetail：DB 不存在时缓存空值标记并抛异常")
    void getProductDetail_shouldCacheNullWhenDbMiss() throws Exception {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn(null);
        given(redissonClient.getLock("lock:goods:1")).willReturn(rLock);
        given(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).willReturn(true);
        given(productMapper.selectById(1L)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> productService.getProductDetail(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        then(valueOperations).should().set(eq(CACHE_KEY), eq("NULL"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("createProduct：分类不存在时抛 CATEGORY_NOT_FOUND")
    void createProduct_shouldThrowWhenCategoryMissing() {
        // given
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductName("new");
        req.setCategoryId(999L);
        req.setOriginalPrice(new BigDecimal("9.99"));
        req.setStock(10);
        given(categoryMapper.selectById(999L)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> productService.createProduct(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        then(productMapper).should(never()).insert(any(Product.class));
    }

    @Test
    @DisplayName("createProduct：分类禁用时抛 CATEGORY_DISABLED")
    void createProduct_shouldThrowWhenCategoryDisabled() {
        // given
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductName("new");
        req.setCategoryId(101L);
        req.setOriginalPrice(new BigDecimal("9.99"));
        req.setStock(10);
        Category disabled = buildCategory();
        disabled.setStatus(0);
        given(categoryMapper.selectById(101L)).willReturn(disabled);

        // when / then
        assertThatThrownBy(() -> productService.createProduct(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_DISABLED);
    }

    @Test
    @DisplayName("createProduct：合法请求写入商品并返回 VO")
    void createProduct_shouldInsertAndReturnVo() {
        // given
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductName("new");
        req.setCategoryId(101L);
        req.setOriginalPrice(new BigDecimal("9.99"));
        req.setStock(10);
        req.setDescription("<script>x</script>");
        given(categoryMapper.selectById(101L)).willReturn(buildCategory());


        // when
        ProductVO vo = productService.createProduct(req);

        // then
        assertThat(vo.getProductName()).isEqualTo("new");
        assertThat(vo.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        then(productMapper).should().insert(any(Product.class));
    }
}
