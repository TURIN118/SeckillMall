package com.seckill.mall.util;

import java.util.Map;
import java.util.Set;

/**
 * 商品列表排序参数白名单过滤工具类。
 * <p>
 * 将前端传入的 sortBy / sortOrder 归一化为 Mapper 支持的标准字段与方向，
 * 防止 SQL 注入；非法值回退为默认值。
 * <p>
 * 与 ProductMapper.xml ORDER BY 支持的字段对齐。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSortUtil.java
 * 邮箱：nj651217@163.com
 */
public final class ProductSortUtil {

    /** Mapper XML 实际支持的标准排序字段 */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "sales", "createTime");
    /** 前端常用别名 → Mapper 标准字段 的归一化映射 */
    private static final Map<String, String> SORT_FIELD_ALIASES = Map.of(
            "salesCount", "sales",
            "originalPrice", "price",
            "id", "createTime"
    );
    /** 默认排序字段（非法值回退） */
    private static final String DEFAULT_SORT_FIELD = "createTime";
    /** 合法排序方向 */
    private static final Set<String> ALLOWED_SORT_ORDERS = Set.of("asc", "desc");
    /** 默认排序方向（非法值回退） */
    private static final String DEFAULT_SORT_ORDER = "desc";

    private ProductSortUtil() {
    }

    /**
     * 白名单过滤排序字段，防 SQL 注入。
     * 将前端传入的 sortBy 归一化为 Mapper 支持的标准字段(price/sales/createTime)：
     * 1. 空值 → 默认值 createTime
     * 2. 命中别名映射(如 salesCount→sales, originalPrice→price, id→createTime) → 标准字段
     * 3. 命中白名单(price/sales/createTime) → 原值
     * 4. 其他非法值 → 默认值 createTime
     */
    public static String sanitizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_FIELD;
        }
        String trimmed = sortBy.trim();
        // 先查别名映射
        String normalized = SORT_FIELD_ALIASES.get(trimmed);
        if (normalized != null) {
            return normalized;
        }
        // 再查白名单
        if (ALLOWED_SORT_FIELDS.contains(trimmed)) {
            return trimmed;
        }
        return DEFAULT_SORT_FIELD;
    }

    /**
     * 白名单过滤排序方向，防 SQL 注入。
     * 将 sortOrder 归一化为小写 asc/desc，非法值回退为默认值 desc。
     * 兼容前端传入的 ASC/DESC 大写形式。
     */
    public static String sanitizeSortOrder(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return DEFAULT_SORT_ORDER;
        }
        String lower = sortOrder.trim().toLowerCase();
        if (ALLOWED_SORT_ORDERS.contains(lower)) {
            return lower;
        }
        return DEFAULT_SORT_ORDER;
    }
}