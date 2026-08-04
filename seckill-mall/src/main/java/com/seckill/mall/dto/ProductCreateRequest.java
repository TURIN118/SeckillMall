package com.seckill.mall.dto;

import com.seckill.mall.entity.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductCreateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductCreateRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称最大 100 字符")
    private String productName;

    @NotNull(message = "分类 ID 不能为空")
    private Long categoryId;

    @Size(max = 500, message = "商品简述最大 500 字符")
    private String description;

    /**
     * 商品详情富文本(HTML)，由 wangEditor 产生。
     * <p>
     * H18 安全说明：本 DTO 不做清洗以保留合法标签，<b>必须在 Service 层使用 Jsoup 清洗</b>，
     * 示例：
     * <pre>{@code
     * import org.jsoup.Jsoup;
     * import org.jsoup.safety.Safelist;
     * String cleanHtml = Jsoup.clean(detailHtml, Safelist.relaxed()
     *     .addTags("div", "span", "img", "p", "br", "strong", "em", "ul", "ol", "li", "h1", "h2", "h3")
     *     .addAttributes("img", "src", "alt", "width", "height")
     *     .addAttributes("a", "href", "target")
     *     .addProtocols("img", "src", "https", "http")
     *     .addProtocols("a", "href", "https", "http"));
     * }</pre>
     */
    private String detailHtml;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
    private BigDecimal originalPrice;

    private List<String> images;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能小于 0")
    private Integer stock;

    private ProductStatus status = ProductStatus.ON_SALE;
}
