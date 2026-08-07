package com.seckill.mall.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果包装
 * <p>
 * M-D7 修复：{@code total} 字段由 {@code long} 改为 {@code int}。
 * <p>
 * 背景：{@code JacksonConfig} 全局将 {@code Long}/{@code long} 序列化为 String
 * （防止雪花 ID 在 JS 端精度丢失），导致 {@code total} 也被序列化为 String，
 * 而前端 {@code PaginationWrapper} 期望 {@code total} 为 Number。
 * 将 {@code total} 改为 {@code int} 后，Jackson 不会对其应用 ToStringSerializer，
 * 从而序列化为 JSON number。
 * <p>
 * 容量说明：{@code int} 最大值约 21 亿，分页总数远不会溢出。
 * {@code of} 方法仍接受 {@code long} 入参以兼容 MyBatis-Plus {@code IPage.getTotal()}，
 * 内部窄化为 {@code int}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：PageResult.java
 * 邮箱：nj651217@163.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    /** 数据列表 */
    private List<T> list;

    /** 总记录数（序列化为 JSON number，而非 string） */
    private int total;

    /** 当前页码 */
    private long pageNum;

    /** 每页大小 */
    private long pageSize;

    /** 总页数 */
    private long pages;

    /**
     * 构造分页结果
     *
     * @param list     数据列表
     * @param total    总记录数（long 入参兼容 IPage.getTotal()，内部窄化为 int）
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        int totalInt = (int) total;
        long pages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
        return new PageResult<>(list, totalInt, pageNum, pageSize, pages);
    }
}
