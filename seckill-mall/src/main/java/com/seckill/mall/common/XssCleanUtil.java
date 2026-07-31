package com.seckill.mall.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：XssCleanUtil.java
 * 邮箱：nj651217@163.com
 */
public final class XssCleanUtil {

    private XssCleanUtil() {
    }

    /**
     * 宽松清洗：保留基本格式标签（a/b/i/em/strong 等），用于商品描述等富文本场景。
     */
    public static String clean(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, Safelist.relaxed());
    }

    /**
     * 严格清洗：移除全部 HTML 标签，仅保留纯文本，用于敏感展示字段。
     */
    public static String cleanStrict(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, Safelist.none());
    }
}
