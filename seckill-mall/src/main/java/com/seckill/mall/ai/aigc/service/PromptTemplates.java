package com.seckill.mall.ai.aigc.service;

import org.springframework.stereotype.Component;

/**
 * AIGC 文案生成 Prompt 模板（T15）。
 * <p>针对四种生成类型（TITLE/DESCRIPTION/DETAIL/SEO）构建用户提示词，
 * 统一约束 LLM「真实客观，不要编造」以防幻觉。
 *
 * <h3>模板设计原则</h3>
 * <ul>
 *   <li>所有模板均显式声明「不要编造未提供的属性、参数、功效」</li>
 *   <li>price 可能为 null，统一用三元运算兜底为 0.0</li>
 *   <li>skuAttributes / categoryName 可能为空，模板中对空值给出降级表述</li>
 *   <li>每种类型对输出格式严格约束（字数/格式/JSON 结构），便于前端直接渲染</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：PromptTemplates.java
 * 邮箱：nj651217@163.com
 */
@Component
public class PromptTemplates {

    /** 系统提示词：电商文案生成助手身份 + 防幻觉总约束 */
    public static final String SYSTEM_PROMPT = "你是电商文案生成助手";

    /** 防幻觉公共约束：所有模板尾部追加 */
    private static final String ANTI_HALLUCINATION = """
            
            【重要约束】
            1. 必须真实客观，不要编造任何未在输入中提供的属性、参数、功效、认证、荣誉。
            2. 不得使用「最」「第一」「国家级」「极品」「顶级」「王者」「霸主」「万能」「包治百病」「疗效」等绝对化或违禁词。
            3. 不得杜撰品牌合作、明星代言、检测报告、专利编号等未经核实的信息。
            4. 只返回要求的文案本身，不要包含解释、前后缀、Markdown 代码块标记。""";

    /**
     * 构建商品标题 prompt。
     * <p>要求 ≤30 字，突出卖点，包含核心属性，只返回标题文本。
     *
     * @param categoryName  分类名（可为 null）
     * @param skuAttributes SKU 属性 JSON（可为 null）
     * @param price         价格（可为 null）
     * @return 用户提示词
     */
    public String buildTitle(String categoryName, String skuAttributes, Double price) {
        double p = price == null ? 0.0 : price;
        String cat = categoryName == null ? "未指定" : categoryName;
        String sku = skuAttributes == null ? "无" : skuAttributes;
        return """
                请为以下商品生成一个电商商品标题：
                - 分类：%s
                - SKU 属性：%s
                - 价格：%.2f 元

                【输出要求】
                1. 标题 ≤ 30 个汉字。
                2. 突出核心卖点，包含 1-2 个关键属性（如颜色/版本/规格）。
                3. 只返回标题文本，不要引号、不要解释、不要换行。
                %s""".formatted(cat, sku, p, ANTI_HALLUCINATION);
    }

    /**
     * 构建商品简介/卖点短文 prompt。
     * <p>要求 ≤100 字，简明扼要描述商品卖点。
     *
     * @param categoryName  分类名（可为 null）
     * @param skuAttributes SKU 属性 JSON（可为 null）
     * @param price         价格（可为 null）
     * @return 用户提示词
     */
    public String buildDescription(String categoryName, String skuAttributes, Double price) {
        double p = price == null ? 0.0 : price;
        String cat = categoryName == null ? "未指定" : categoryName;
        String sku = skuAttributes == null ? "无" : skuAttributes;
        return """
                请为以下商品生成一段商品简介/卖点短文：
                - 分类：%s
                - SKU 属性：%s
                - 价格：%.2f 元

                【输出要求】
                1. 简介 ≤ 100 个汉字。
                2. 用 2-3 句话概括商品核心卖点与适用场景。
                3. 语言简洁有吸引力，面向消费者。
                4. 只返回简介文本，不要换行分段、不要 Markdown。
                %s""".formatted(cat, sku, p, ANTI_HALLUCINATION);
    }

    /**
     * 构建商品详情 prompt。
     * <p>要求 HTML 格式，{@code <ul>} 列卖点，{@code <table>} 列参数，只返回 HTML。
     *
     * @param categoryName  分类名（可为 null）
     * @param skuAttributes SKU 属性 JSON（可为 null）
     * @param price         价格（可为 null）
     * @return 用户提示词
     */
    public String buildDetail(String categoryName, String skuAttributes, Double price) {
        double p = price == null ? 0.0 : price;
        String cat = categoryName == null ? "未指定" : categoryName;
        String sku = skuAttributes == null ? "无" : skuAttributes;
        return """
                请为以下商品生成商品详情 HTML：
                - 分类：%s
                - SKU 属性：%s
                - 价格：%.2f 元

                【输出要求】
                1. 输出合法 HTML 片段，包含：
                   - 一个 <h3> 小标题（如「核心卖点」）
                   - 一个 <ul> 列出 3-5 条卖点
                   - 一个 <table> 列出商品参数（参数名+参数值两列），参数必须来自输入的 SKU 属性，不得编造
                2. 不要输出 <html>/<body> 等外层标签，不要输出 Markdown 代码块标记。
                3. 只返回 HTML 片段本身。
                %s""".formatted(cat, sku, p, ANTI_HALLUCINATION);
    }

    /**
     * 构建 SEO 关键词和描述 prompt。
     * <p>要求 JSON 格式 {@code {"keywords":"...","description":"..."}}。
     *
     * @param categoryName  分类名（可为 null）
     * @param skuAttributes SKU 属性 JSON（可为 null）
     * @param price         价格（可为 null）
     * @return 用户提示词
     */
    public String buildSeo(String categoryName, String skuAttributes, Double price) {
        double p = price == null ? 0.0 : price;
        String cat = categoryName == null ? "未指定" : categoryName;
        String sku = skuAttributes == null ? "无" : skuAttributes;
        return """
                请为以下商品生成 SEO 关键词和描述：
                - 分类：%s
                - SKU 属性：%s
                - 价格：%.2f 元

                【输出要求】
                1. 输出严格 JSON，结构如下：
                   {"keywords":"关键词1,关键词2,关键词3","description":"SEO 描述"}
                2. keywords：3-8 个关键词，英文逗号分隔，覆盖分类+核心属性+卖点。
                3. description：≤ 80 个汉字的 SEO 网页描述。
                4. 只返回 JSON 本身，不要 Markdown 代码块标记、不要解释。
                %s""".formatted(cat, sku, p, ANTI_HALLUCINATION);
    }
}