package com.seckill.mall.ai.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.ai.assistant.dto.ProductSearchInput;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.dto.ProductSummaryDTO;
import com.seckill.mall.product.api.query.ProductListQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.stereotype.Component;

/**
 * AI 导购助手 function-calling 工具：商品搜索。
 * <p>大模型理解用户自然语言需求后，调用本工具搜索真实商品库，返回结构化商品列表，
 * 再由大模型基于真实数据生成自然语言推荐文案（避免 LLM 编造商品）。
 *
 * <h3>Spring AI 1.0.0-M3 API 适配说明</h3>
 * <ul>
 *   <li>1.0.0-M3 <b>不存在</b> {@code @Tool} 注解（1.0.0-M4+ 才引入），亦无 {@code ChatClient.tools(Object...)} 方法。</li>
 *   <li>本版本使用 {@link FunctionCallbackWrapper#builder(java.util.function.Function)} 构建工具回调，
 *       通过 {@code .withInputType(ProductSearchInput.class)} 让 Spring AI 自动生成 JSON Schema 暴露给大模型。</li>
 *   <li>多参数通过 {@link ProductSearchInput} DTO 包装（M3 的 {@code Function<I,O>} 仅支持单入参）。</li>
 *   <li>响应通过 {@code .withResponseConverter()} 将 {@link PageResult} 序列化为 JSON 字符串回传给大模型。</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * FunctionCallback cb = productSearchTool.buildFunctionCallback();
 * aiGatewayService.stream(systemPrompt, userPrompt, "shopping-assistant", cb);
 * }</pre>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSearchTool.java
 * 邮箱：nj651217@163.com
 */
@Component
public class ProductSearchTool {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchTool.class);

    /** 工具名称（大模型 function-calling 调用名） */
    public static final String TOOL_NAME = "searchProducts";
    /** 工具描述（暴露给大模型，引导其何时调用本工具） */
    public static final String TOOL_DESCRIPTION =
            "搜索商品：按关键词/分类/价格区间筛选，返回商品列表（含商品名、价格、库存、销量、图片等）。"
                    + "当用户描述购物需求时调用本工具获取真实在售商品，再基于返回结果生成推荐。";

    /** 单页最大条数上限，防止 LLM 传入过大值拖慢 DB */
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ProductApi productApi;
    private final ObjectMapper objectMapper;

    public ProductSearchTool(ProductApi productApi, ObjectMapper objectMapper) {
        this.productApi = productApi;
        this.objectMapper = objectMapper;
    }

    /**
     * 实际执行商品搜索的业务方法。
     * <p>将 {@link ProductSearchInput} 映射为 {@link ProductListQuery}，
     * 调 {@link ProductApi#listProducts} 返回 {@link PageResult}。
     *
     * @param input 工具入参
     * @return 分页商品结果
     */
    public PageResult<ProductSummaryDTO> searchProducts(ProductSearchInput input) {
        log.info("AI 工具 searchProducts 调用：keyword={} categoryId={} price=[{},{}] page={}/{}",
                input.getKeyword(), input.getCategoryId(),
                input.getMinPrice(), input.getMaxPrice(),
                input.getPageNum(), input.getPageSize());

        // 分页参数兜底：pageNum>=1，pageSize 在 [1, 50]
        int pageNum = input.getPageNum() == null || input.getPageNum() < 1 ? 1 : input.getPageNum();
        int pageSize = input.getPageSize() == null || input.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(input.getPageSize(), MAX_PAGE_SIZE);

        // 直接用 ProductListQuery.builder 构建查询，消除旧分层 ProductQueryRequest 依赖。
        // 注意：原 ProductQueryRequest 的 sortBy/sortOrder/minPrice/maxPrice 字段在
        // ProductListQuery 中不存在（ProductApiConverter.toProductListQuery 也未映射），
        // 原本就被丢弃，此处不再保留。
        ProductListQuery query = ProductListQuery.builder()
                .keyword(input.getKeyword())
                .categoryId(input.getCategoryId())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .status("ON_SALE")
                .build();

        return productApi.listProducts(query);
    }

    /**
     * 构建 Spring AI 1.0.0-M3 的 {@link FunctionCallback}，注册到大模型 function-calling。
     * <p>用 {@link FunctionCallbackWrapper#builder} 包装 {@link #searchProducts}，
     * 设置工具名/描述/入参类型/响应转换器（PageResult → JSON 字符串）。
     *
     * @return FunctionCallback 实例
     */
    public FunctionCallback buildFunctionCallback() {
        return FunctionCallbackWrapper.<ProductSearchInput, PageResult<ProductSummaryDTO>>builder(
                        input -> searchProducts(input))
                .withName(TOOL_NAME)
                .withDescription(TOOL_DESCRIPTION)
                .withInputType(ProductSearchInput.class)
                .withResponseConverter(this::serializeResult)
                .build();
    }

    /**
     * 将 {@link PageResult} 序列化为 JSON 字符串回传给大模型。
     * <p>序列化异常时返回错误提示字符串，避免工具调用整体失败。
     */
    private String serializeResult(PageResult<ProductSummaryDTO> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("AI 工具 searchProducts 响应序列化失败：{}", e.getMessage(), e);
            return "{\"error\":\"商品搜索结果序列化失败，请重试\"}";
        }
    }
}